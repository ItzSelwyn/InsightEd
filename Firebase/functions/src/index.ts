import * as admin from "firebase-admin";
import { GoogleGenerativeAI, SchemaType } from "@google/generative-ai";
import { defineSecret } from "firebase-functions/params";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";

if (!admin.apps.length) {
  admin.initializeApp();
}

const database = admin.database();
const geminiApiKey = defineSecret("GEMINI_API_KEY");
const CLASS_TIME_ZONE = "Asia/Kolkata";

const GEMINI_MODEL = "gemini-1.5-flash";
const MIN_PRESENCE_RATIO = 0.3;
const MIN_PRESENT_RATIO = 0.75;

// Duration source of truth for each class period.
// This is more reliable than deriving duration from sparse BLE events.
const PERIOD_DURATION_SECONDS: Record<string, number> = {
  period1: 3600,
  period2: 3600,
  period3: 3600,
  period4: 3600,
  period5: 3600,
  period6: 3600,
};

const attendanceVerdictSchema: any = {
  type: SchemaType.OBJECT,
  properties: {
    verdict: {
      type: SchemaType.STRING,
      enum: ["present", "flagged"],
    },
    confidence: {
      type: SchemaType.INTEGER,
      minimum: 0,
      maximum: 100,
    },
    explanation: {
      type: SchemaType.STRING,
    },
  },
  required: ["verdict", "confidence", "explanation"],
};

type AttendanceEventType = "ble_seen" | "ble_lost" | "face_verify";

type AttendanceEvent = {
  timestamp: number;
  type: AttendanceEventType;
  confidence?: number;
};

type GeminiVerdict = {
  verdict: "present" | "flagged";
  confidence: number;
  explanation: string;
};

type AiVerdict = GeminiVerdict["verdict"] | "insufficient_duration";

type ProcessedStudent = {
  uuid: string;
  total_present_seconds: number;
  period_duration_seconds: number;
  presence_ratio: number;
  gap_count: number;
  longest_gap_seconds: number;
  ai_verdict: AiVerdict;
  ai_confidence: number;
  ai_explanation: string;
  timeline: string;
};

type AttendanceRequest = {
  date?: unknown;
  period?: unknown;
};

type ReappearanceSegment = {
  seenAt: number;
  faceVerifiedAfter: boolean;
};

function asString(value: unknown): string {
  return typeof value === "string" ? value.trim() : "";
}

function asNumber(value: unknown): number | null {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }

  if (typeof value === "string" && value.trim() !== "") {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }

  return null;
}

function formatClock(unixSeconds: number): string {
  return new Intl.DateTimeFormat("en-US", {
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
    timeZone: CLASS_TIME_ZONE,
  })
    .format(new Date(unixSeconds * 1000))
    .replace(" ", "")
    .toLowerCase();
}

function formatDuration(seconds: number): string {
  const safeSeconds = Math.max(0, Math.round(seconds));

  if (safeSeconds < 60) {
    return `${safeSeconds} second${safeSeconds === 1 ? "" : "s"}`;
  }

  const totalMinutes = Math.round(safeSeconds / 60);
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;

  if (hours === 0) {
    return `${minutes} minute${minutes === 1 ? "" : "s"}`;
  }

  if (minutes === 0) {
    return `${hours} hour${hours === 1 ? "" : "s"}`;
  }

  return `${hours} hour${hours === 1 ? "" : "s"} ${minutes} minute${minutes === 1 ? "" : "s"}`;
}

function formatPercent(ratio: number): string {
  return `${Math.round(ratio * 100)}%`;
}

function getPeriodDurationSeconds(period: string): number {
  const durationSeconds = PERIOD_DURATION_SECONDS[period];

  if (typeof durationSeconds !== "number" || durationSeconds <= 0) {
    throw new HttpsError("invalid-argument", `No duration configured for ${period}`);
  }

  return durationSeconds;
}

function normalizeEvent(value: unknown): AttendanceEvent | null {
  if (!value || typeof value !== "object") {
    return null;
  }

  const record = value as Record<string, unknown>;
  const timestamp = asNumber(record.timestamp);
  const type = asString(record.type) as AttendanceEventType;

  if (timestamp === null) {
    return null;
  }

  if (type !== "ble_seen" && type !== "ble_lost" && type !== "face_verify") {
    return null;
  }

  const confidence = asNumber(record.confidence);

  return {
    timestamp,
    type,
    ...(confidence === null ? {} : { confidence }),
  };
}

function loadEvents(studentSnapshot: admin.database.DataSnapshot): AttendanceEvent[] {
  const eventsValue = studentSnapshot.child("events").val();
  if (!eventsValue || typeof eventsValue !== "object") {
    return [];
  }

  return Object.values(eventsValue as Record<string, unknown>)
    .map(normalizeEvent)
    .filter((event): event is AttendanceEvent => event !== null)
    .sort((left, right) => left.timestamp - right.timestamp);
}

function buildTimelineAndMetrics(
  events: AttendanceEvent[],
  captureEndSeconds: number,
): {
  timeline: string;
  totalPresentSeconds: number;
  gapCount: number;
  longestGapSeconds: number;
} {
  const sentences: string[] = [];
  let totalPresentSeconds = 0;
  let gapCount = 0;
  let longestGapSeconds = 0;

  let currentPresenceStart: number | null = null;
  let currentLostAt: number | null = null;
  let reappearanceSegment: ReappearanceSegment | null = null;

  for (const event of events) {
    if (event.type === "face_verify") {
      const confidenceText =
        typeof event.confidence === "number"
          ? ` (confidence ${Math.round(event.confidence)})`
          : "";
      sentences.push(`Face verified at ${formatClock(event.timestamp)}${confidenceText}.`);

      if (reappearanceSegment && event.timestamp >= reappearanceSegment.seenAt) {
        reappearanceSegment.faceVerifiedAfter = true;
      }
      continue;
    }

    if (event.type === "ble_seen") {
      if (currentLostAt !== null) {
        const gapSeconds = event.timestamp - currentLostAt;
        longestGapSeconds = Math.max(longestGapSeconds, Math.max(0, gapSeconds));
        sentences.push(
          `BLE signal lost at ${formatClock(currentLostAt)} for ${formatDuration(gapSeconds)}.`,
        );
        sentences.push(`Reappeared at ${formatClock(event.timestamp)}.`);

        if (reappearanceSegment) {
          sentences.push(
            reappearanceSegment.faceVerifiedAfter
              ? `Face re-verification happened after the reappearance at ${formatClock(reappearanceSegment.seenAt)}.`
              : `No re-verification was recorded after the reappearance at ${formatClock(reappearanceSegment.seenAt)}.`,
          );
        }

        currentLostAt = null;

        reappearanceSegment = {
          seenAt: event.timestamp,
          faceVerifiedAfter: false,
        };
      } else {
        sentences.push(`BLE seen at ${formatClock(event.timestamp)}.`);

        if (currentPresenceStart === null) {
          currentPresenceStart = event.timestamp;
        }
      }

      if (currentLostAt === null && currentPresenceStart === null) {
        currentPresenceStart = event.timestamp;
      }

      continue;
    }

    if (event.type === "ble_lost") {
      gapCount += 1;

      if (currentPresenceStart !== null) {
        totalPresentSeconds += Math.max(0, event.timestamp - currentPresenceStart);
        currentPresenceStart = null;
      }

      if (reappearanceSegment) {
        sentences.push(
          reappearanceSegment.faceVerifiedAfter
            ? `Face re-verification happened after the reappearance at ${formatClock(reappearanceSegment.seenAt)}.`
            : `No re-verification was recorded after the reappearance at ${formatClock(reappearanceSegment.seenAt)}.`,
        );
        reappearanceSegment = null;
      }

      currentLostAt = event.timestamp;
      sentences.push(`BLE signal lost at ${formatClock(event.timestamp)}.`);
    }
  }

  if (currentPresenceStart !== null) {
    totalPresentSeconds += Math.max(0, captureEndSeconds - currentPresenceStart);
  }

  if (currentLostAt !== null) {
    sentences.push(`BLE signal was still lost at the end of the period, last seen loss at ${formatClock(currentLostAt)}.`);
  }

  if (reappearanceSegment) {
    sentences.push(
      reappearanceSegment.faceVerifiedAfter
        ? `Face re-verification happened after the last reappearance at ${formatClock(reappearanceSegment.seenAt)}.`
        : `No re-verification was recorded after the last reappearance at ${formatClock(reappearanceSegment.seenAt)}.`,
    );
  }

  if (captureEndSeconds > 0) {
    sentences.push(`Period processed through ${formatClock(captureEndSeconds)}.`);
  }

  return {
    timeline: sentences.join(" "),
    totalPresentSeconds,
    gapCount,
    longestGapSeconds,
  };
}

function buildGeminiPrompt(params: {
  date: string;
  period: string;
  uuid: string;
  timeline: string;
  totalPresentSeconds: number;
  periodDurationSeconds: number;
  presenceRatio: number;
  gapCount: number;
  longestGapSeconds: number;
}): string {
  return [
    "You are reviewing classroom attendance for a single student.",
    `Date: ${params.date}`,
    `Period: ${params.period}`,
    `UUID: ${params.uuid}`,
    `Timeline: ${params.timeline}`,
    `Computed stats: ${JSON.stringify({
      total_present_seconds: params.totalPresentSeconds,
      period_duration_seconds: params.periodDurationSeconds,
      presence_ratio: params.presenceRatio,
      gap_count: params.gapCount,
      longest_gap_seconds: params.longestGapSeconds,
    })}`,
    "Decide if this is normal attendance or suspicious proxy attendance.",
    "Return only JSON that matches the provided schema.",
  ].join("\n\n");
}

async function reviewWithGemini(params: {
  apiKey: string;
  date: string;
  period: string;
  uuid: string;
  timeline: string;
  totalPresentSeconds: number;
  periodDurationSeconds: number;
  presenceRatio: number;
  gapCount: number;
  longestGapSeconds: number;
}): Promise<GeminiVerdict> {
  const genAI = new GoogleGenerativeAI(params.apiKey);
  const model = genAI.getGenerativeModel({
    model: GEMINI_MODEL,
    generationConfig: {
      temperature: 0.2,
      responseMimeType: "application/json",
      responseSchema: attendanceVerdictSchema,
    } as any,
  });

  const prompt = buildGeminiPrompt(params);
  const result = await model.generateContent(prompt);
  const text = result.response.text().trim();

  return JSON.parse(text) as GeminiVerdict;
}

async function processStudentAttendance(
  date: string,
  period: string,
  uuid: string,
  geminiApiKeyValue: string,
): Promise<ProcessedStudent> {
  const studentRef = database.ref(`attendance/${date}/${period}/${uuid}`);
  const studentSnapshot = await studentRef.get();

  if (!studentSnapshot.exists()) {
    throw new HttpsError("not-found", `No attendance node found for ${uuid}`);
  }

  const summarySnapshot = studentSnapshot.child("summary");
  const existingLastSeen = asNumber(summarySnapshot.child("last_seen").val());
  const events = loadEvents(studentSnapshot);
  const fallbackEndSeconds = existingLastSeen ?? events[events.length - 1]?.timestamp ?? 0;
  const timelineData = buildTimelineAndMetrics(events, fallbackEndSeconds);
  const periodDurationSeconds = getPeriodDurationSeconds(period);
  const presenceRatio = periodDurationSeconds > 0
    ? timelineData.totalPresentSeconds / periodDurationSeconds
    : 0;

  let aiVerdict: AiVerdict;
  let aiConfidence: number;
  let aiExplanation: string;

  if (presenceRatio < MIN_PRESENCE_RATIO) {
    aiVerdict = "insufficient_duration";
    aiConfidence = Math.max(
      5,
      Math.min(100, Math.round((1 - (presenceRatio / MIN_PRESENCE_RATIO)) * 100)),
    );
    aiExplanation = `Present for only ${formatPercent(presenceRatio)} of the period — too brief to confirm attendance.`;
  } else {
    const fallbackVerdict: GeminiVerdict = timelineData.gapCount > 0
      ? {
          verdict: "flagged",
          confidence: 55,
          explanation: "Attendance contains BLE losses or reappearance gaps; manual review is recommended.",
        }
      : {
          verdict: "present",
          confidence: 85,
          explanation: "Continuous BLE presence with no suspicious gaps was detected.",
        };

    let modelVerdict = fallbackVerdict;

    try {
      if (geminiApiKeyValue) {
        modelVerdict = await reviewWithGemini({
          apiKey: geminiApiKeyValue,
          date,
          period,
          uuid,
          timeline: timelineData.timeline,
          totalPresentSeconds: timelineData.totalPresentSeconds,
          periodDurationSeconds,
          presenceRatio,
          gapCount: timelineData.gapCount,
          longestGapSeconds: timelineData.longestGapSeconds,
        });
      }
    } catch (error) {
      logger.error("Gemini review failed, using fallback verdict", { date, period, uuid, error });
    }

    aiVerdict = modelVerdict.verdict;
    aiConfidence = modelVerdict.confidence;
    aiExplanation = modelVerdict.explanation;

    if (aiVerdict === "present" && presenceRatio < MIN_PRESENT_RATIO) {
      aiVerdict = "flagged";
      aiExplanation = `${aiExplanation} (overridden: presence ratio below required threshold)`;
      aiConfidence = Math.max(10, Math.min(100, Math.round(aiConfidence * presenceRatio / MIN_PRESENT_RATIO)));
    }
  }

  await studentRef.child("summary").update({
    total_present_seconds: timelineData.totalPresentSeconds,
    period_duration_seconds: periodDurationSeconds,
    presence_ratio: presenceRatio,
    gap_count: timelineData.gapCount,
    longest_gap_seconds: timelineData.longestGapSeconds,
    ai_verdict: aiVerdict,
    ai_confidence: aiConfidence,
    ai_explanation: aiExplanation,
    status: aiVerdict,
  });

  logger.info("Processed attendance summary", {
    date,
    period,
    uuid,
    total_present_seconds: timelineData.totalPresentSeconds,
    period_duration_seconds: periodDurationSeconds,
    presence_ratio: presenceRatio,
    gap_count: timelineData.gapCount,
    longest_gap_seconds: timelineData.longestGapSeconds,
    ai_verdict: aiVerdict,
    ai_confidence: aiConfidence,
  });

  return {
    uuid,
    total_present_seconds: timelineData.totalPresentSeconds,
    period_duration_seconds: periodDurationSeconds,
    presence_ratio: presenceRatio,
    gap_count: timelineData.gapCount,
    longest_gap_seconds: timelineData.longestGapSeconds,
    ai_verdict: aiVerdict,
    ai_confidence: aiConfidence,
    ai_explanation: aiExplanation,
    timeline: timelineData.timeline,
  };
}

export const processPeriodAttendance = onCall(
  {
    secrets: [geminiApiKey],
    cors: true,
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "You must be authenticated to run attendance processing.");
    }

    const date = asString((request.data as AttendanceRequest | undefined)?.date);
    const period = asString((request.data as AttendanceRequest | undefined)?.period);

    if (!/^\d{2}_\d{2}_\d{4}$/.test(date)) {
      throw new HttpsError("invalid-argument", "date must be in dd_mm_yyyy format.");
    }

    if (!/^period\d+$/.test(period)) {
      throw new HttpsError("invalid-argument", "period must look like period1, period2, etc.");
    }

    const periodRef = database.ref(`attendance/${date}/${period}`);
    const periodSnapshot = await periodRef.get();

    if (!periodSnapshot.exists()) {
      return {
        date,
        period,
        processed_students: 0,
        flagged_students: 0,
        students: [] as ProcessedStudent[],
      };
    }

    const uuids = periodSnapshot.exists() ? Object.keys((periodSnapshot.val() as Record<string, unknown>) ?? {}) : [];

    const apiKeyValue = geminiApiKey.value();
    const results = await Promise.all(
      uuids.map((uuid) => processStudentAttendance(date, period, uuid, apiKeyValue)),
    );

    const flaggedStudents = results.filter((student) => student.ai_verdict === "flagged").length;

    return {
      date,
      period,
      processed_students: results.length,
      flagged_students: flaggedStudents,
      students: results,
    };
  },
);
