// ===== PATH ASSETS =====
const ASSETS_PATH =
  "file:///storage/emulated/0/Android/media/com.example.nstimelypray/assets"

/* ======================
BAGIAN: TANGGAL MASEHI & HIJRIAH
====================== */
const now = new Date()
const bulanIndo = [
  "Januari",
  "Februari",
  "Maret",
  "April",
  "Mei",
  "Juni",
  "Juli",
  "Agustus",
  "September",
  "Oktober",
  "November",
  "Desember",
]
const tglNowIndo = `${now.getDate()} ${
  bulanIndo[now.getMonth()]
} ${now.getFullYear()}`

/* ======================
    BAGIAN: JAM DIGITAL
====================== */
function updateClock() {
  const n = new Date()
  const hh = String(n.getHours()).padStart(2, "0")
  const mm = String(n.getMinutes()).padStart(2, "0")

  // jam utama
  const mainClock = document.getElementById("clock")
  if (mainClock) mainClock.textContent = `${hh}:${mm}`

  // jam di popup adzan
  const popupClock = document.getElementById("popup-clock")
  if (popupClock) popupClock.textContent = `${hh}:${mm}`
}
setInterval(updateClock, 1000)
updateClock()

/* ======================
    BAGIAN: KONFIGURASI API
====================== */
const KOTA = 1634 // ID kota (ubah sesuai kebutuhan)
const TAHUN = 2025
const STORAGE_KEY = `jadwalSholat-${TAHUN}-${KOTA}`
let jadwalData = null
let schedule = []

/* ======================
    BAGIAN: SYNC JADWAL + HIJRI
====================== */
async function doSync() {
  const fill = document.getElementById("progress-fill")
  const text = document.getElementById("progress-text")
  const retryBtn = document.getElementById("retry-btn")
  const startBtn = document.getElementById("start-sync-btn")

  retryBtn.classList.add("hidden")
  startBtn.classList.add("hidden")
  fill.style.width = "0%"
  text.textContent = `Sinkronisasi jadwal sholat 0/12 bulan…`

  const data = {}
  let successCount = 0

  /* ======================
    1) SYNC JADWAL SHOLAT
====================== */
  for (let m = 1; m <= 12; m++) {
    const mm = String(m).padStart(2, "0")
    try {
      const res = await fetch(
        `https://api.myquran.com/v2/sholat/jadwal/${KOTA}/${TAHUN}/${mm}`
      )
      if (!res.ok) throw new Error("HTTP error")
      const json = await res.json()
      data[mm] = json.data.jadwal
      successCount++
      fill.style.width = `${(successCount / 12) * 100}%`
      text.textContent = `Sinkronisasi jadwal sholat ${successCount}/12 bulan…`
    } catch (err) {
      console.error(err)
      text.textContent =
        "❌ Gagal sinkronisasi jadwal sholat. Pastikan perangkat online."
      retryBtn.classList.remove("hidden")
      return
    }
  }

  // simpan jadwal sholat
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
  localStorage.setItem(`${STORAGE_KEY}-timestamp`, new Date().toISOString())
  jadwalData = data

  /* ======================
    2) SYNC TANGGAL HIJRI
====================== */
  text.textContent = `Sinkronisasi tanggal hijriah…`
  fill.style.width = "0%"

  const hijriMap = {}
  const startDate = new Date(`${TAHUN}-01-01`)
  const endDate = new Date(`${TAHUN}-12-31`)
  const totalDays =
    Math.floor((endDate - startDate) / (1000 * 60 * 60 * 24)) + 1
  let dayCount = 0

  for (let i = 0; i < totalDays; i++) {
    const current = new Date(startDate)
    current.setDate(startDate.getDate() + i)

    const tglStr = formatDate(current) // YYYY-MM-DD
    try {
      const res = await fetch(
        `https://api.myquran.com/v2/cal/hijr/${tglStr}/adj=-1`
      )
      const json = await res.json()
      if (json.status) {
        hijriMap[tglStr] = json.data.date[1] // contoh: "17 Dzulhijjah 1445 H"
      }
    } catch (err) {
      console.error("Gagal ambil hijri:", tglStr, err)
    }

    dayCount++
    fill.style.width = `${(dayCount / totalDays) * 100}%`
    text.textContent = `Sinkronisasi tanggal hijriah ${dayCount}/${totalDays} hari…`
  }

  localStorage.setItem(
    "hijriMap",
    JSON.stringify({
      year: parseInt(TAHUN),
      data: hijriMap,
    })
  )

  /* ======================
    3) SELESAI
====================== */
  text.textContent = "✅ Sinkronisasi selesai!"
  setTimeout(() => {
    document.getElementById("sync-overlay").classList.add("hidden")
    renderTodaySchedule()
    showHijriToday()
  }, 1000)
}

/* ======================
    UTIL
====================== */
function formatDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, "0")
  const d = String(date.getDate()).padStart(2, "0")
  return `${y}-${m}-${d}`
}

function showHijriToday() {
  const today = new Date()
  const tglStr = formatDate(today)
  const tglNowIndo = today.toLocaleDateString("id-ID", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  })

  let hijriStorage = localStorage.getItem("hijriMap")
  if (!hijriStorage) return
  hijriStorage = JSON.parse(hijriStorage)

  if (hijriStorage.year !== today.getFullYear()) return

  const hijriToday = hijriStorage.data[tglStr]
  if (hijriToday) {
    // ambil semua elemen dengan class tgl-masehi
    document.querySelectorAll(".tgl-masehi").forEach((el) => {
      el.textContent = tglNowIndo
    })

    document.querySelectorAll(".tgl-hijri").forEach((el) => {
      el.textContent = hijriToday
    })
  }
}

/* ======================
    BAGIAN: LOAD DATA
====================== */
function loadJadwal() {
  const raw = localStorage.getItem(STORAGE_KEY)
  const hijriRaw = localStorage.getItem("hijriMap")
  const today = new Date()

  // ✅ Cek hijriMap.year
  if (hijriRaw) {
    try {
      const hijriData = JSON.parse(hijriRaw)
      if (hijriData.year !== today.getFullYear()) {
        console.warn(
          "⚠️ Tahun HijriMap tidak sesuai tahun sekarang. Reset localStorage…"
        )
        localStorage.removeItem(STORAGE_KEY)
        localStorage.removeItem(`${STORAGE_KEY}-timestamp`)
        localStorage.removeItem("hijriMap")
        document.getElementById("sync-overlay").classList.remove("hidden")
        return
      }
    } catch (e) {
      console.error("Error parsing hijriMap:", e)
      localStorage.removeItem("hijriMap")
      document.getElementById("sync-overlay").classList.remove("hidden")
      return
    }
  }

  // ✅ Lanjut load jadwal sholat kalau ada
  if (raw) {
    jadwalData = JSON.parse(raw)
    renderTodaySchedule()
    showHijriToday()
  } else {
    document.getElementById("sync-overlay").classList.remove("hidden")
  }
}

document.getElementById("start-sync-btn").onclick = doSync
document.getElementById("retry-btn").onclick = doSync

/* ======================
    BAGIAN: RENDER JADWAL HARI INI
====================== */
function renderTodaySchedule() {
  const today = new Date()
  const mm = String(today.getMonth() + 1).padStart(2, "0")
  const dd = String(today.getDate()).padStart(2, "0")
  const daily = jadwalData[mm].find((d) => d.date === `${TAHUN}-${mm}-${dd}`)

  const boxes = [
    { name: "Imsak", time: daily.imsak },
    { name: "Subuh", time: daily.subuh },
    { name: "Syuruk", time: daily.terbit },
    { name: "Dzuhur", time: daily.dzuhur },
    { name: "Ashar", time: daily.ashar },
    { name: "Maghrib", time: daily.maghrib },
    { name: "Isya", time: daily.isya },
  ]

  const container = document.getElementById("prayer-boxes")
  container.innerHTML = ""
  boxes.forEach((b) => {
    const div = document.createElement("div")
    div.className = "box"
    div.innerHTML = `<div class="label">${b.name.toUpperCase()}</div><div class="time">${
      b.time
    }</div>`
    container.appendChild(div)
  })

  schedule = boxes.map((b) => ({ name: b.name.toUpperCase(), time: b.time }))
}

/* ======================
    BAGIAN: HIGHLIGHT BOX
====================== */
function highlightPrayerBoxes() {
  if (!schedule.length) return

  const now = new Date()
  const boxes = document.querySelectorAll(".box")

  boxes.forEach((b, i) => {
    const [h, m] = schedule[i].time.split(":").map(Number)
    const start = new Date()
    start.setHours(h, m, 0, 0)
    const end = new Date(start.getTime() + 15 * 60 * 1000)

    now >= start && now <= end
      ? b.classList.add("active")
      : b.classList.remove("active")
  })
}
setInterval(highlightPrayerBoxes, 30 * 1000) // cukup 30 detik sekali

/* ======================
    BAGIAN: COUNTDOWN MENUJU SHOLAT
====================== */
function toTodayDate(hhmm) {
  const d = new Date()
  const [h, m] = hhmm.split(":").map(Number)
  d.setHours(h, m, 0, 0)
  return d
}
function getNextTarget() {
  const now = new Date()
  for (const item of schedule) {
    const t = toTodayDate(item.time)
    if (t > now) return { label: item.name, at: t }
  }
  const t = toTodayDate(schedule[0].time)
  t.setDate(t.getDate() + 1)
  return { label: schedule[0].name, at: t }
}
function tickCountdown() {
  if (!schedule.length) return
  const { label, at } = getNextTarget()
  document.getElementById("c-label").textContent = label
  const diff = Math.max(0, Math.floor((at - new Date()) / 1000))
  const h = String(Math.floor(diff / 3600)).padStart(2, "0")
  const m = String(Math.floor((diff % 3600) / 60)).padStart(2, "0")
  const s = String(diff % 60).padStart(2, "0")
  document.getElementById("c-time").textContent = `${h}:${m}:${s}`
}
setInterval(tickCountdown, 1000)

/* ======================
    BAGIAN: MAIN ADZAN
====================== */
let lastPlayed = null // supaya tidak double play
function checkAndPlayAdzan() {
  if (!schedule.length) return

  const now = new Date()
  const hhmmNow = `${String(now.getHours()).padStart(2, "0")}:${String(
    now.getMinutes()
  ).padStart(2, "0")}`
  const prayerAdzan = ["SUBUH", "DZUHUR", "ASHAR", "MAGHRIB", "ISYA"]

  schedule.forEach((prayer) => {
    if (
      prayerAdzan.includes(prayer.name) &&
      prayer.time === hhmmNow &&
      lastPlayed !== prayer.name
    ) {
      prayer.name === "SUBUH"
        ? document.getElementById("adzan-subuh").play()
        : document.getElementById("adzan-regular").play()
      lastPlayed = prayer.name
    }
  })
}
setInterval(checkAndPlayAdzan, 1000)

/* ======================
    START APP
====================== */
loadJadwal()
highlightPrayerBoxes()

/* ======================
    BAGIAN: POPUP SLIDESHOW
====================== */
// ===== Popup & Slideshow =====
const popup = document.getElementById("adzan-popup")
const popupText = document.getElementById("popup-text")
const popupLabel = document.getElementById("c-labelpopup")
const popupTimer = document.getElementById("c-timepopup")
const boxTimer = document.getElementById("popup-timer")

const slide1 = document.getElementById("slide1")
const slide2 = document.getElementById("slide2")

// daftar gambar
const slideImages = Array.from(
  { length: 12 },
  (_, i) => `${ASSETS_PATH}/${i + 1}.jpg`
)

// preload supaya mulus
;(function preloadImages(list) {
  list.forEach((src) => {
    const im = new Image()
    im.src = src
  })
})(slideImages)

// state
let slideIndex = 0 // index gambar berikutnya
let currentLayer = 1 // 1 atau 2 (layer aktif)
let slideIntervalId = null // id interval
let isSlideshowRunning = false
let isPopupVisible = false

const HOLD_MS = 10000 // 10 detik per gambar

function applyImage(el, src, activate) {
  if (src) el.style.backgroundImage = `url('${src}')`
  if (activate) el.classList.add("active")
  else el.classList.remove("active")
}

function startSlideshow() {
  if (isSlideshowRunning) return // cegah dobel
  isSlideshowRunning = true
  isPopupVisible = true
  popup.classList.remove("hidden")

  // gambar awal
  slideIndex = 0
  applyImage(slide1, slideImages[slideIndex], true)
  applyImage(slide2, slideImages[(slideIndex + 1) % slideImages.length], false)
  currentLayer = 1
  slideIndex = (slideIndex + 1) % slideImages.length

  // jalan berkala
  slideIntervalId = setInterval(nextSlide, HOLD_MS)
}

function nextSlide() {
  const nextSrc = slideImages[slideIndex]

  if (currentLayer === 1) {
    applyImage(slide2, nextSrc, true) // fade-in layer 2
    slide1.classList.remove("active") // fade-out layer 1
    currentLayer = 2
  } else {
    applyImage(slide1, nextSrc, true) // fade-in layer 1
    slide2.classList.remove("active") // fade-out layer 2
    currentLayer = 1
  }

  slideIndex = (slideIndex + 1) % slideImages.length
}

function stopSlideshow(keepPopup = true) {
  if (slideIntervalId) clearInterval(slideIntervalId)
  slideIntervalId = null
  isSlideshowRunning = false

  // opsional: hentikan efek aktif agar freeze rapi
  slide1.classList.remove("active")
  slide2.classList.remove("active")

  if (!keepPopup) {
    popup.classList.add("hidden")
    isPopupVisible = false
  }
}

// ===== Integrasi dengan countdown =====
// Tampilkan popup (slideshow) ketika sudah masuk jendela pra-adzan
// -> misal 30 menit sebelum (1800 detik); ubah sesuai kebutuhan.
const PRE_ADZAN_WINDOW_SEC = 1800

function formatTime(sec) {
  const m = String(Math.floor(sec / 60)).padStart(2, "0")
  const s = String(sec % 60).padStart(2, "0")
  return `${m}:${s}`
}

// Perhatikan: kode countdown-mu sudah ada.
// Di bawah ini kita HANYA gunakan untuk trigger popup sekali.
setInterval(() => {
  if (!schedule.length) return
  const now = new Date()

  // cari target terdekat (ikutin logika getNextTarget milikmu)
  let nearest = null
  for (const item of schedule) {
    const [h, m] = item.time.split(":").map(Number)
    const t = new Date()
    t.setHours(h, m, 0, 0)
    const diff = Math.floor((t - now) / 1000)
    if (diff > 0) {
      nearest = { name: item.name, diff }
      break
    }
  }
  if (!nearest) return

  // mulai slideshow HANYA sekali saat masuk window
  if (
    nearest.diff <= PRE_ADZAN_WINDOW_SEC &&
    nearest.diff > 0 &&
    !isPopupVisible
  ) {
    popupLabel.textContent = `${nearest.name} : `
    popupTimer.textContent = formatTime(nearest.diff)
    startSlideshow()
  }

  // update timer kalau popup lagi tampil
  if (isPopupVisible && nearest.diff > 0) {
    popupTimer.textContent = formatTime(nearest.diff)

    if (popupTimer.textContent === "00:01") {
      boxTimer.style.display = "none"
      popupText.style.display = "none"
    }
  }
}, 1000)

// ===== Sinkronisasi UI dengan audio adzan =====
// Saat audio mulai -> hentikan slideshow, ganti teks
const adzanSubuh = document.getElementById("adzan-subuh")
const adzanRegular = document.getElementById("adzan-regular")

function onAdzanStart() {
  boxTimer.style.display = "none"

  // pastikan popup terlihat
  if (!isPopupVisible) {
    popup.classList.remove("hidden")
    isPopupVisible = true
  }

  // hentikan slideshow tapi tetap tampilkan popup
  stopSlideshow(true)

  // buat elemen video
  const video = document.createElement("video")
  video.src = `${ASSETS_PATH}/1.mp4`
  video.autoplay = true
  video.playsInline = true
  video.muted = false // biar suara adzan keluar
  video.controls = false
  video.className = "adzan-video"

  // kosongkan isi slide1 dan masukkan video
  slide1.innerHTML = ""
  slide1.style.opacity = "1"
  slide1.appendChild(video)

  // paksa play
  video.play().catch((err) => {
    console.warn("Autoplay gagal:", err)
  })

  slide2.classList.remove("active")

  // style untuk popupText
  Object.assign(popupText.style, {
    backgroundColor: "#02101c5e",
    borderRadius: "32px",
    height: "20vh",
    fontSize: "3rem",
    fontWeight: "700",
    marginBottom: "5%",
    animation: "slideUp 0.8s ease both",
    letterSpacing: "4px",
    display: "flex",
    alignItems: "center",
    justifyContent: "center", // biar teks di tengah box
    color: "#fff", // opsional biar kontras
    textAlign: "center",
  })

  popupText.textContent = "HARAP TENANG KETIKA ADZAN BERKUMANDANG"
  popupTimer.textContent = ""
}

function onAdzanEnd() {
  // hapus video dan kembalikan slide normal
  slide1.innerHTML = ""
  stopSlideshow(false)
}

adzanSubuh.addEventListener("play", onAdzanStart)
adzanRegular.addEventListener("play", onAdzanStart)
adzanSubuh.addEventListener("ended", onAdzanEnd)
adzanRegular.addEventListener("ended", onAdzanEnd)
