import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();

interface ReportData {
  id?: string;
  type: "Lost" | "Found";
  title: string;
  category: string;
  description: string;
  image_url?: string;
  imageUrl?: string;
  latitude: number;
  longitude: number;
  location_name?: string;
  campus_zone?: string;
  timestamp: number;
  status: string;
  user_id?: string;
  userId?: string;
  user_name?: string;
  user_contact?: string;
}

const EARTH_RADIUS_METERS = 6371000;
const MATCH_THRESHOLD = 75.0; // Ambang batas 75%

/**
 * Trigger Cloud Function: Dipicu setiap ada dokumen laporan baru dibuat di koleksi 'reports'
 */
export const onReportCreated = functions.firestore
  .document("reports/{reportId}")
  .onCreate(async (snap, context) => {
    const newReportId = context.params.reportId;
    const newReport = snap.data() as ReportData;

    console.log(`[onReportCreated] Laporan baru diterima: ${newReportId} (Tipe: ${newReport.type})`);

    const oppositeType = newReport.type === "Lost" ? "Found" : "Lost";

    try {
      // 1. Ambil seluruh laporan aktif dengan tipe berlawanan
      const oppositeSnapshot = await db
        .collection("reports")
        .where("type", "==", oppositeType)
        .where("status", "==", "Open")
        .get();

      if (oppositeSnapshot.empty) {
        console.log(`[onReportCreated] Belum ada laporan bertipe ${oppositeType} untuk dicocokkan.`);
        return null;
      }

      const matchPromises = oppositeSnapshot.docs.map(async (doc) => {
        const candidate = doc.data() as ReportData;
        const candidateId = doc.id;

        const lostReport = newReport.type === "Lost" ? newReport : candidate;
        const foundReport = newReport.type === "Found" ? newReport : candidate;
        const lostId = newReport.type === "Lost" ? newReportId : candidateId;
        const foundId = newReport.type === "Found" ? newReportId : candidateId;

        // Hitung skor komponen
        const textScore = computeTextSimilarity(lostReport, foundReport);
        const timeScore = computeTimeSimilarity(lostReport.timestamp, foundReport.timestamp);
        const locationScore = computeLocationSimilarity(
          lostReport.latitude,
          lostReport.longitude,
          foundReport.latitude,
          foundReport.longitude,
          lostReport.location_name || "",
          foundReport.location_name || ""
        );

        const lostImage = lostReport.image_url || lostReport.imageUrl || "";
        const foundImage = foundReport.image_url || foundReport.imageUrl || "";
        const hasVisual = lostImage.trim().length > 0 && foundImage.trim().length > 0;

        let visualScore = 0.0;
        let totalScore = 0.0;

        // 3. Terapkan Pembobotan Dinamis (Adaptive Weighting)
        if (hasVisual) {
          // Jika foto ada: Visual 40%, Teks 30%, Waktu 15%, Lokasi 15% (Sisa 60%)
          visualScore = lostReport.category === foundReport.category ? 0.85 : 0.50;
          totalScore = (visualScore * 0.40) + (textScore * 0.30) + (timeScore * 0.15) + (locationScore * 0.15);
        } else {
          // Jika foto TIDAK ada: Teks 50%, Waktu 30%, Lokasi 20%
          totalScore = (textScore * 0.50) + (timeScore * 0.30) + (locationScore * 0.20);
        }

        const percentageScore = Math.min(100, Math.max(0, totalScore * 100));

        console.log(`[Candidate: ${candidateId}] Score: ${percentageScore.toFixed(1)}% (Teks: ${textScore.toFixed(2)}, Loc: ${locationScore.toFixed(2)}, Time: ${timeScore.toFixed(2)})`);

        // 4. Jika Total Skor > 75%, Simpan ke koleksi 'matches' & Kirim Notifikasi FCM
        if (percentageScore >= MATCH_THRESHOLD) {
          const matchDocId = `${lostId}_${foundId}`;
          const summaryText = hasVisual
            ? `Multimodal Match: Visual ${(visualScore * 100).toFixed(0)}%, Teks ${(textScore * 100).toFixed(0)}%, Lokasi ${(locationScore * 100).toFixed(0)}%, Waktu ${(timeScore * 100).toFixed(0)}%`
            : `Contextual Match: Teks ${(textScore * 100).toFixed(0)}%, Waktu ${(timeScore * 100).toFixed(0)}%, Lokasi ${(locationScore * 100).toFixed(0)}%`;

          const matchPayload = {
            id: matchDocId,
            lost_report_id: lostId,
            found_report_id: foundId,
            lost_title: lostReport.title,
            found_title: foundReport.title,
            category: lostReport.category || foundReport.category,
            lost_image_url: lostImage,
            found_image_url: foundImage,
            lost_location: lostReport.location_name || "",
            found_location: foundReport.location_name || "",
            text_score: textScore,
            time_score: timeScore,
            location_score: locationScore,
            visual_score: visualScore,
            confidence_score: percentageScore,
            status: "Pending",
            created_at: Date.now(),
            match_summary: summaryText,
            lost_contact: lostReport.user_contact || "",
            found_contact: foundReport.user_contact || "",
          };

          // Simpan ke Firestore
          await db.collection("matches").document(matchDocId).set(matchPayload, { merge: true });

          // Kirim payload FCM ke pengguna
          await notifyUsersFCM(lostReport, foundReport, percentageScore, summaryText);
        }
      });

      await Promise.all(matchPromises);
      return null;
    } catch (error) {
      console.error("[onReportCreated] Error executing matching engine:", error);
      return null;
    }
  });

/**
 * Kirim pesan FCM ke perangkat pemilik barang hilang dan penemu barang
 */
async function notifyUsersFCM(
  lost: ReportData,
  found: ReportData,
  confidenceScore: number,
  summary: string
) {
  const userIds = [lost.user_id || lost.userId, found.user_id || found.userId].filter(Boolean) as string[];
  const tokens: string[] = [];

  for (const uid of userIds) {
    const userDoc = await db.collection("users").document(uid).get();
    if (userDoc.exists) {
      const token = userDoc.data()?.fcm_token;
      if (token) tokens.push(token);
    }
  }

  if (tokens.length === 0) {
    console.log("[FCM] Tidak ditemukan token FCM terdaftar untuk pengguna terkait.");
    return;
  }

  const payload: admin.messaging.MulticastMessage = {
    tokens: tokens,
    notification: {
      title: "Peluang Kecocokan Ditemukan!",
      body: `Sistem mendeteksi kecocokan ${confidenceScore.toFixed(0)}% antara '${lost.title}' dan '${found.title}'`,
    },
    data: {
      confidence_score: confidenceScore.toFixed(0),
      lost_title: lost.title,
      found_title: found.title,
      match_summary: summary,
      type: "MATCH_ALERT",
    },
    android: {
      priority: "high",
      notification: {
        channelId: "unesa_lost_found_channel",
        sound: "default",
      },
    },
  };

  try {
    const response = await messaging.sendEachForMulticast(payload);
    console.log(`[FCM] Berhasil mengirimkan ${response.successCount} notifikasi ke Android.`);
  } catch (err) {
    console.error("[FCM] Gagal mengirim pesan notifikasi:", err);
  }
}

/**
 * NLP Teks Sederhana: Tokenisasi Jaccard + Kategori
 */
function computeTextSimilarity(r1: ReportData, r2: ReportData): number {
  const catBonus = r1.category.toLowerCase() === r2.category.toLowerCase() ? 0.35 : 0.0;
  const stopWords = new Set(["yang", "di", "ke", "dari", "ini", "itu", "dan", "atau", "pada", "ada", "saya", "telah", "hilang", "ditemukan", "barang", "unesa"]);

  const tokens1 = tokenize(`${r1.title} ${r1.description}`, stopWords);
  const tokens2 = tokenize(`${r2.title} ${r2.description}`, stopWords);

  if (tokens1.size === 0 || tokens2.size === 0) {
    return Math.min(1.0, catBonus * 1.5);
  }

  let intersection = 0;
  tokens1.forEach((t) => {
    if (tokens2.has(t)) intersection++;
  });

  const union = new Set([...tokens1, ...tokens2]).size;
  const jaccard = union > 0 ? intersection / union : 0.0;

  return Math.min(1.0, Math.max(0.0, jaccard * 0.65 + catBonus));
}

function tokenize(text: string, stopWords: Set<string>): Set<string> {
  const words = text
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, " ")
    .split(/\s+/)
    .filter((w) => w.length > 2 && !stopWords.has(w));
  return new Set(words);
}

/**
 * Kedekatan Waktu (Time Decay)
 */
function computeTimeSimilarity(t1: number, t2: number): number {
  const diffHours = Math.abs(t1 - t2) / (1000 * 60 * 60);
  if (diffHours <= 6) return 1.0;
  if (diffHours <= 24) return 0.90;
  if (diffHours <= 48) return 0.75;
  if (diffHours <= 120) return 0.50;
  if (diffHours <= 240) return 0.30;
  return 0.15;
}

/**
 * Kedekatan Lokasi (Haversine Formula)
 */
function computeLocationSimilarity(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number,
  loc1: string,
  loc2: string
): number {
  if (lat1 !== 0 && lon1 !== 0 && lat2 !== 0 && lon2 !== 0) {
    const dist = haversineDistanceMeters(lat1, lon1, lat2, lon2);
    if (dist <= 50) return 1.0;
    if (dist <= 200) return 0.90;
    if (dist <= 500) return 0.80;
    if (dist <= 1000) return 0.65;
    if (dist <= 2500) return 0.40;
    return 0.15;
  }

  // Fallback nama lokasi
  if (loc1 && loc2 && loc1.toLowerCase() === loc2.toLowerCase()) {
    return 0.95;
  }
  return 0.30;
}

function haversineDistanceMeters(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const toRad = (x: number) => (x * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const rLat1 = toRad(lat1);
  const rLat2 = toRad(lat2);

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(rLat1) * Math.cos(rLat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return EARTH_RADIUS_METERS * c;
}
