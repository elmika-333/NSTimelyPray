// ===== ELEMENT SETTING ADZAN =====
const settingsPopup = document.getElementById("settings-popup")

const toggleShowPreAdzan = document.getElementById("toggle-show-pre-adzan")
const toggleShowIqomah = document.getElementById("toggle-show-iqomah")

const preAdzanSettings = document.getElementById("pre-adzan-settings")
const preAdzanTime = document.getElementById("pre-adzan-time")

const iqomahSettings = document.getElementById("iqomah-settings")
const iqomahTime = document.getElementById("iqomah-time")

const toggleAdzanBtn = document.getElementById("toggle-adzan")
const toggleIqomahBtn = document.getElementById("toggle-iqomah")

const preAdzanInput = document.getElementById("pre-adzan-sec")
const iqomahDelayInput = document.getElementById("iqomah-delay-sec")

const saveBtn = document.getElementById("save-settings")
const closeBtn = document.getElementById("close-settings")

// ===== Load settings dari localStorage =====
function loadSettings() {
  const raw = localStorage.getItem("adzanSettings")
  if (!raw)
    return {
      showPreAdzan: true,
      showIqomah: true,
      playAdzan: true,
      playIqomah: true,
      preAdzanSec: 60,
      iqomahDelaySec: 30,
    }
  try {
    return JSON.parse(raw)
  } catch (e) {
    console.error("Error parsing settings:", e)
    return {
      showPreAdzan: true,
      showIqomah: true,
      playAdzan: true,
      playIqomah: true,
      preAdzanSec: 60,
      iqomahDelaySec: 30,
    }
  }
}

// ===== Isi popup sesuai settings =====
function loadSettingsToPopup() {
  const settings = loadSettings()

  // Jika toggle utama mati, otomatis set nilai detail
  if (!settings.showPreAdzan) {
    settings.playAdzan = false
    settings.preAdzanSec = 0
  }
  if (!settings.showIqomah) {
    settings.playIqomah = false
    settings.iqomahDelaySec = 0
  }

  toggleShowPreAdzan.classList.toggle("active", !!settings.showPreAdzan)
  toggleShowIqomah.classList.toggle("active", !!settings.showIqomah)

  preAdzanSettings.classList.toggle("hidden", !settings.showPreAdzan)
  preAdzanTime.classList.toggle("hidden", !settings.showPreAdzan)
  iqomahSettings.classList.toggle("hidden", !settings.showIqomah)
  iqomahTime.classList.toggle("hidden", !settings.showIqomah)

  toggleAdzanBtn.classList.toggle("active", !!settings.playAdzan)
  toggleIqomahBtn.classList.toggle("active", !!settings.playIqomah)

  preAdzanInput.value = settings.preAdzanSec
  iqomahDelayInput.value = settings.iqomahDelaySec
}

// ===== Event buka popup =====
document.querySelectorAll(".topbar, #adzan-popup .topbar").forEach((el) => {
  el.addEventListener("dblclick", (e) => {
    e.preventDefault()
    loadSettingsToPopup()
    settingsPopup.classList.remove("hidden")
  })
})

// ===== Toggle utama =====
function toggleMain(btn, details) {
  btn.classList.toggle("active")
  details.forEach((el) => el.classList.toggle("hidden"))
}

toggleShowPreAdzan.addEventListener("click", () =>
  toggleMain(toggleShowPreAdzan, [preAdzanSettings, preAdzanTime])
)
toggleShowIqomah.addEventListener("click", () =>
  toggleMain(toggleShowIqomah, [iqomahSettings, iqomahTime])
)

// ===== Toggle detail =====
function toggleButton(btn) {
  btn.classList.toggle("active")
}
toggleAdzanBtn.addEventListener("click", () => toggleButton(toggleAdzanBtn))
toggleIqomahBtn.addEventListener("click", () => toggleButton(toggleIqomahBtn))

// ===== Simpan setting =====
saveBtn.addEventListener("click", () => {
  const newSettings = {
    showPreAdzan: toggleShowPreAdzan.classList.contains("active"),
    showIqomah: toggleShowIqomah.classList.contains("active"),
    playAdzan: toggleAdzanBtn.classList.contains("active"),
    playIqomah: toggleIqomahBtn.classList.contains("active"),
    preAdzanSec: Number(preAdzanInput.value),
    iqomahDelaySec: Number(iqomahDelayInput.value),
  }

  if (!newSettings.showPreAdzan) {
    newSettings.playAdzan = false
    newSettings.preAdzanSec = 0
  }
  if (!newSettings.showIqomah) {
    newSettings.playIqomah = false
    newSettings.iqomahDelaySec = 0
  }

  localStorage.setItem("adzanSettings", JSON.stringify(newSettings))
  settingsPopup.classList.add("hidden")
})

// ===== Tutup popup tanpa simpan =====
closeBtn.addEventListener("click", () => {
  settingsPopup.classList.add("hidden")
})

// ====================== BAGIAN: JAM, TANGGAL, JADWAL ======================
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

function updateClock() {
  const n = new Date()
  const hh = String(n.getHours()).padStart(2, "0")
  const mm = String(n.getMinutes()).padStart(2, "0")
  const mainClock = document.getElementById("clock")
  if (mainClock) mainClock.textContent = `${hh}:${mm}`
  const popupClock = document.getElementById("popup-clock")
  if (popupClock) popupClock.textContent = `${hh}:${mm}`
}
setInterval(updateClock, 1000)
updateClock()

const KOTA = 1634
const TAHUN = 2025
const STORAGE_KEY = `jadwalSholat-${TAHUN}-${KOTA}`
let jadwalData = null
let schedule = []

// ====================== SYNC JADWAL & HIJRI ======================
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

  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
  localStorage.setItem(`${STORAGE_KEY}-timestamp`, new Date().toISOString())
  jadwalData = data

  // Hijri sync
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
    const tglStr = formatDate(current)
    try {
      const res = await fetch(
        `https://api.myquran.com/v2/cal/hijr/${tglStr}/adj=-1`
      )
      const json = await res.json()
      if (json.status) hijriMap[tglStr] = json.data.date[1]
    } catch (err) {
      console.error("Gagal ambil hijri:", tglStr, err)
    }
    dayCount++
    fill.style.width = `${(dayCount / totalDays) * 100}%`
    text.textContent = `Sinkronisasi tanggal hijriah ${dayCount}/${totalDays} hari…`
  }

  localStorage.setItem(
    "hijriMap",
    JSON.stringify({ year: TAHUN, data: hijriMap })
  )

  text.textContent = "✅ Sinkronisasi selesai!"
  setTimeout(() => {
    document.getElementById("sync-overlay").classList.add("hidden")
    renderTodaySchedule()
    showHijriToday()
  }, 1000)
}

function formatDate(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(
    2,
    "0"
  )}-${String(date.getDate()).padStart(2, "0")}`
}

function showHijriToday() {
  const today = new Date()
  const tglStr = formatDate(today)
  let hijriStorage = localStorage.getItem("hijriMap")
  if (!hijriStorage) return
  hijriStorage = JSON.parse(hijriStorage)
  if (hijriStorage.year !== today.getFullYear()) return
  const hijriToday = hijriStorage.data[tglStr]
  if (hijriToday) {
    document.querySelectorAll(".tgl-masehi").forEach((el) => {
      el.textContent = today.toLocaleDateString("id-ID", {
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric",
      })
    })
    document.querySelectorAll(".tgl-hijri").forEach((el) => {
      el.textContent = hijriToday
    })
  }
}

function loadJadwal() {
  const raw = localStorage.getItem(STORAGE_KEY)
  const hijriRaw = localStorage.getItem("hijriMap")
  const today = new Date()

  if (hijriRaw) {
    try {
      const hijriData = JSON.parse(hijriRaw)
      if (hijriData.year !== today.getFullYear()) {
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

// ====================== RENDER JADWAL HARI INI ======================
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
    { name: "Ashar", time: "14:50" },
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
setInterval(highlightPrayerBoxes, 1500)
loadJadwal()
highlightPrayerBoxes()

// ====================== COUNTDOWN ======================
function toTodayDate(hhmm) {
  const d = new Date()
  ;[h, m] = hhmm.split(":").map(Number)
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

// ====================== POPUP & SLIDESHOW ======================
const popup = document.getElementById("adzan-popup")
const popupText = document.getElementById("popup-text")
const popupLabel = document.getElementById("c-labelpopup")
const popupTimer = document.getElementById("c-timepopup")
const boxTimer = document.getElementById("popup-timer")

const slide1 = document.getElementById("slide1")
const slide2 = document.getElementById("slide2")
const slideImages = Array.from(
  { length: 12 },
  (_, i) => `assets/slideshow/${i + 1}.jpg`
)
;(function preloadImages(list) {
  list.forEach((src) => {
    const im = new Image()
    im.src = src
  })
})(slideImages)

let slideIndex = 0,
  currentLayer = 1,
  slideIntervalId = null,
  isSlideshowRunning = false,
  isPopupVisible = false
const HOLD_MS = 10000

function applyImage(el, src, activate) {
  if (src) el.style.backgroundImage = `url('${src}')`
  el.classList.toggle("active", activate)
}

function startSlideshow() {
  if (isSlideshowRunning) return
  isSlideshowRunning = true
  isPopupVisible = true
  popup.classList.remove("hidden")
  slideIndex = 0
  applyImage(slide1, slideImages[slideIndex], true)
  applyImage(slide2, slideImages[(slideIndex + 1) % slideImages.length], false)
  currentLayer = 1
  slideIndex = (slideIndex + 1) % slideImages.length
  slideIntervalId = setInterval(nextSlide, HOLD_MS)
}

function nextSlide() {
  const nextSrc = slideImages[slideIndex]
  if (currentLayer === 1) {
    applyImage(slide2, nextSrc, true)
    slide1.classList.remove("active")
    currentLayer = 2
  } else {
    applyImage(slide1, nextSrc, true)
    slide2.classList.remove("active")
    currentLayer = 1
  }
  slideIndex = (slideIndex + 1) % slideImages.length
}

function stopSlideshow(keepPopup = true) {
  if (slideIntervalId) clearInterval(slideIntervalId)
  slideIntervalId = null
  isSlideshowRunning = false
  slide1.classList.remove("active")
  slide2.classList.remove("active")
  if (!keepPopup) {
    popup.classList.add("hidden")
    isPopupVisible = false
  }
}

function formatTime(sec) {
  const m = String(Math.floor(sec / 60)).padStart(2, "0")
  const s = String(sec % 60).padStart(2, "0")
  return `${m}:${s}`
}

// ====================== Integrasi countdown & popup ======================
setInterval(() => {
  if (!schedule.length) return
  const settings = loadSettings()
  if (!settings.showPreAdzan) {
    if (isPopupVisible) stopSlideshow(false)
    return
  }
  const now = new Date()
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
  const preSec = settings.preAdzanSec || 0
  if (preSec > 0 && nearest.diff <= preSec && !isPopupVisible) {
    popupLabel.textContent = `${nearest.name} : `
    popupTimer.textContent = formatTime(nearest.diff)
    startSlideshow()
  }
  if (isPopupVisible && nearest.diff > 0) {
    popupTimer.textContent = formatTime(nearest.diff)
    if (popupTimer.textContent === "00:01") {
      boxTimer.style.display = "none"
      popupText.style.display = "none"
    }
  }
}, 1000)

// ====================== ADZAN VIDEO & AUDIO ======================
const adzanSubuh = document.getElementById("adzan-subuh")
const adzanRegular = document.getElementById("adzan-regular")

function onAdzanStart() {
  const settings = loadSettings()

  // popup muncul
  boxTimer.style.display = "none"
  if (!isPopupVisible) {
    popup.classList.remove("hidden")
    isPopupVisible = true
  }

  // stop slideshow biar nggak override lagi
  stopSlideshow(true)

  // kosongkan isi slide1 & slide2 total
  slide1.innerHTML = ""
  slide2.innerHTML = ""
  slide1.style.backgroundImage = "none"
  slide2.style.backgroundImage = "none"

  // buat elemen video
  const video = document.createElement("video")
  video.src = "assets/video/1.mp4"
  video.autoplay = true
  video.loop = true
  video.playsInline = true
  video.controls = false
  video.muted = false // video tetap ada suara ambience, audio adzan mp3 yg di-mute/unmute
  video.className = "adzan-video"

  Object.assign(video.style, {
    width: "100%",
    height: "100%",
    objectFit: "cover",
    display: "block",
    position: "absolute",
    top: "0",
    left: "0",
    zIndex: "0",
  })

  slide1.appendChild(video)
  slide1.style.opacity = "1"
  slide1.classList.add("active")
  slide2.classList.remove("active")

  video.play().catch((err) => console.warn("Video autoplay blocked:", err))

  // teks popup
  popupText.textContent = "HARAP TENANG KETIKA ADZAN BERKUMANDANG"
  popupTimer.textContent = ""
  Object.assign(popupText.style, {
    backgroundColor: "rgba(0, 16, 28, 0.37)",
    borderRadius: "2vw",
    height: "20vh",
    fontSize: "2.5vw",
    fontWeight: "700",
    marginBottom: "5%",
    animation: "slideUp 0.8s ease both",
    letterSpacing: "4px",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    color: "#ffffff",
    textAlign: "center",
    border: "0.2vw solid #fffdfd",
  })

  // atur suara adzan (mp3), mute/unmute sesuai setting
  adzanSubuh.muted = !settings.playAdzan
  adzanRegular.muted = !settings.playAdzan
}

function onAdzanEnd() {
  stopSlideshow(false)
  popup.classList.add("hidden")
  isPopupVisible = false
}

adzanSubuh.addEventListener("play", onAdzanStart)
adzanRegular.addEventListener("play", onAdzanStart)
adzanSubuh.addEventListener("ended", onAdzanEnd)
adzanRegular.addEventListener("ended", onAdzanEnd)

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
      if (prayer.name === "SUBUH") {
        adzanSubuh.currentTime = 0
        adzanSubuh.play().catch((e) => console.log("Audio blocked:", e))
      } else {
        adzanRegular.currentTime = 0
        adzanRegular.play().catch((e) => console.log("Audio blocked:", e))
      }
      lastPlayed = prayer.name
    }
  })
}
setInterval(checkAndPlayAdzan, 1000)
