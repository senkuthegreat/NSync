import React, { useState, useEffect, useRef } from 'react';
import {
  Download,
  Star,
  ExternalLink,
  ShieldCheck,
  Percent,
  LayoutGrid,
  WifiOff,
  Sparkles,
  ArrowRight,
  Clock,
  MapPin,
  Calendar,
  ScanLine,
  CheckCircle2,
  Terminal,
  Zap,
  BookOpen
} from 'lucide-react';

export default function App() {
  const [scrolled, setScrolled] = useState(false);
  const [activeTab, setActiveTab] = useState('attendance');
  const [activeDay, setActiveDay] = useState('Wed');
  const [tilt, setTilt] = useState({ x: 0, y: 0 });
  const [release, setRelease] = useState({
    tag: 'v1.0.10',
    size: '31.4 MB',
    url: 'https://github.com/senkuthegreat/NSync/releases/latest/download/NSync-latest.apk',
  });

  const phoneRef = useRef(null);

  // Scroll listener for header elevation
  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Fetch real-time release info from GitHub API
  useEffect(() => {
    fetch('https://api.github.com/repos/senkuthegreat/NSync/releases/latest')
      .then((res) => (res.ok ? res.json() : Promise.reject()))
      .then((data) => {
        const apkAsset = data.assets?.find(
          (a) => a.name.endsWith('.apk') && a.name.includes('latest')
        ) || data.assets?.find((a) => a.name.endsWith('.apk'));

        if (apkAsset) {
          const mbSize = (apkAsset.size / (1024 * 1024)).toFixed(1);
          setRelease({
            tag: data.tag_name || 'v1.0.10',
            size: `${mbSize} MB`,
            url: apkAsset.browser_download_url,
          });
        }
      })
      .catch(() => {
        // Fallback already pre-set
      });
  }, []);

  // 3D Parallax Tilt Handler
  const handleMouseMove = (e) => {
    if (!phoneRef.current) return;
    const rect = phoneRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    const centerX = rect.width / 2;
    const centerY = rect.height / 2;

    const tiltX = -((y - centerY) / centerY) * 12; // deg
    const tiltY = ((x - centerX) / centerX) * 14; // deg

    setTilt({ x: tiltX, y: tiltY });
  };

  const handleMouseLeave = () => {
    setTilt({ x: 0, y: 0 });
  };

  return (
    <div className="app-container">
      {/* Ambient Lighting Background */}
      <div className="bg-ambient">
        <div className="ambient-glow-1"></div>
        <div className="ambient-glow-2"></div>
        <div className="ambient-glow-3"></div>
        <div className="bg-grid-overlay"></div>
      </div>

      {/* Header (Minimal, Big Logo, Unkempt Font, Zero Nav Links, Zero Borders) */}
      <header className={scrolled ? 'scrolled' : ''}>
        <div className="nav-container">
          <a href="#" className="brand">
            <img src="./assets/logo.png" alt="NSync Logo" className="brand-logo" />
            <span className="brand-text">
              N<span>Sync</span>
            </span>
          </a>

          <a href={release.url} className="nav-cta-btn">
            <Download size={18} />
            <span>Download APK</span>
          </a>
        </div>
      </header>

      {/* Main Content */}
      <main>
        {/* Hero Section */}
        <section className="hero-section">
          <div className="hero-grid">
            {/* Left Column: Hero Copy & CTA */}
            <div className="hero-content">
              <div className="version-pill">
                <span className="pulsing-dot"></span>
                <span>
                  Version {release.tag} is Live • NSUT Student Companion
                </span>
              </div>

              <h1 className="hero-title">
                Academic Life,
                <br />
                <span className="gradient-text">Seamlessly Synced.</span>
              </h1>

              <p className="hero-subtitle">
                The companion app NSUT students always wanted. Instant attendance
                tracking with 75% target projection, live daily timetables, exam
                hall details, and zero captcha friction.
              </p>

              <div className="hero-actions">
                <a href={release.url} className="btn-download-primary">
                  <div className="btn-inner">
                    <Download size={22} />
                    <span>Download APK ({release.tag})</span>
                  </div>
                  <span className="btn-meta">
                    Android 7.0+ • ~{release.size} • Free & Open Source
                  </span>
                </a>

                <a
                  href="https://github.com/senkuthegreat/NSync"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="btn-secondary"
                >
                  <Star size={18} />
                  <span>Star on GitHub</span>
                </a>
              </div>

              <div className="hero-stats">
                <div className="stat-item">
                  <span className="stat-val">100%</span>
                  <span className="stat-label">Offline Accessible</span>
                </div>
                <div className="stat-item">
                  <span className="stat-val">0</span>
                  <span className="stat-label">Captcha Logins Needed</span>
                </div>
                <div className="stat-item">
                  <span className="stat-val">&lt; 1s</span>
                  <span className="stat-label">Instant Cold Start</span>
                </div>
              </div>
            </div>

            {/* Right Column: 3D Interactive Phone Mockup */}
            <div className="phone-showcase" id="mockup">
              <div
                className="phone-wrapper"
                ref={phoneRef}
                onMouseMove={handleMouseMove}
                onMouseLeave={handleMouseLeave}
                style={{
                  transform: `rotateX(${tilt.x}deg) rotateY(${tilt.y}deg)`,
                }}
              >
                <div className="phone-screen">
                  {/* Punch Hole Island */}
                  <div className="phone-island"></div>

                  <div className="screen-content">
                    {/* Phone Status Bar */}
                    <div className="mock-status-bar">
                      <span>09:41</span>
                      <span>5G • 100%</span>
                    </div>

                    {/* App Header Inside Phone */}
                    <div className="mock-header">
                      <div className="mock-greeting">
                        <h4>Hey, Senku 👋</h4>
                        <p>COE • 6th Semester • NSUT</p>
                      </div>
                      <div className="mock-sync-badge">Synced</div>
                    </div>

                    {/* Tab 1: Attendance */}
                    {activeTab === 'attendance' && (
                      <div className="app-screen">
                        <div className="mock-attendance-card">
                          <div className="circular-gauge">
                            <svg className="gauge-svg" viewBox="0 0 100 100">
                              <circle
                                className="gauge-bg"
                                cx="50"
                                cy="50"
                                r="40"
                              />
                              <circle
                                className="gauge-progress"
                                cx="50"
                                cy="50"
                                r="40"
                                style={{ strokeDashoffset: '42' }}
                              />
                            </svg>
                            <div className="gauge-text">
                              <span className="gauge-number">83.3%</span>
                              <span className="gauge-status">SAFE</span>
                            </div>
                          </div>
                          <p style={{ fontSize: '11px', color: '#94a3b8' }}>
                            You can safely skip <strong>4 lecture(s)</strong>{' '}
                            above 75%.
                          </p>
                        </div>

                        <div className="mock-subject-list">
                          <div className="mock-subject-item">
                            <div className="subject-header">
                              <span>Compiler Design</span>
                              <span style={{ color: '#38bdf8' }}>91.2%</span>
                            </div>
                            <div className="progress-bar-bg">
                              <div
                                className="progress-bar-fill"
                                style={{ width: '91%' }}
                              ></div>
                            </div>
                          </div>

                          <div className="mock-subject-item">
                            <div className="subject-header">
                              <span>Computer Networks</span>
                              <span style={{ color: '#38bdf8' }}>85.7%</span>
                            </div>
                            <div className="progress-bar-bg">
                              <div
                                className="progress-bar-fill"
                                style={{ width: '85%' }}
                              ></div>
                            </div>
                          </div>

                          <div className="mock-subject-item">
                            <div className="subject-header">
                              <span>Machine Learning</span>
                              <span style={{ color: '#fbbf24' }}>76.0%</span>
                            </div>
                            <div className="progress-bar-bg">
                              <div
                                className="progress-bar-fill"
                                style={{ width: '76%', background: '#fbbf24' }}
                              ></div>
                            </div>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Tab 2: Timetable */}
                    {activeTab === 'timetable' && (
                      <div className="app-screen">
                        <div className="mock-timetable-container">
                          <div className="mock-day-pill">
                            {['Mon', 'Tue', 'Wed', 'Thu', 'Fri'].map((day) => (
                              <div
                                key={day}
                                className={`day-badge ${
                                  activeDay === day ? 'active' : ''
                                }`}
                                onClick={() => setActiveDay(day)}
                                style={{ cursor: 'pointer' }}
                              >
                                {day}
                              </div>
                            ))}
                          </div>

                          <div className="timetable-card">
                            <div className="tt-time">09:00 - 10:00 AM</div>
                            <div className="tt-title">Cloud Computing (L)</div>
                            <div className="tt-room">Hall: APJ-102 • Dr. Verma</div>
                          </div>

                          <div className="timetable-card">
                            <div className="tt-time">10:00 - 11:00 AM</div>
                            <div className="tt-title">Distributed Systems</div>
                            <div className="tt-room">Hall: APJ-104 • Prof. Gupta</div>
                          </div>

                          <div className="timetable-card">
                            <div className="tt-time">11:15 - 01:15 PM</div>
                            <div className="tt-title">Networks Lab (P2)</div>
                            <div className="tt-room">Computer Centre • Lab 4</div>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Tab 3: Exams */}
                    {activeTab === 'exams' && (
                      <div className="app-screen">
                        <div className="mock-exam-container">
                          <div className="exam-hall-ticket">
                            <div className="exam-date-tag">
                              18 NOV 2026 • 10:00 AM
                            </div>
                            <div className="exam-course-name">
                              Mid-Term: Artificial Intelligence
                            </div>
                            <div className="exam-seating-meta">
                              <span>Room: SPS-04</span>
                              <span>Row: B • Desk: 14</span>
                            </div>
                          </div>

                          <div className="exam-hall-ticket">
                            <div className="exam-date-tag">
                              21 NOV 2026 • 02:00 PM
                            </div>
                            <div className="exam-course-name">
                              Mid-Term: Compiler Design
                            </div>
                            <div className="exam-seating-meta">
                              <span>Room: APJ-201</span>
                              <span>Row: C • Desk: 08</span>
                            </div>
                          </div>

                          <div className="exam-hall-ticket">
                            <div className="exam-date-tag">
                              24 NOV 2026 • 10:00 AM
                            </div>
                            <div className="exam-course-name">
                              Mid-Term: Operating Systems
                            </div>
                            <div className="exam-seating-meta">
                              <span>Room: SPS-12</span>
                              <span>Row: A • Desk: 02</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Tab 4: OCR */}
                    {activeTab === 'ocr' && (
                      <div className="app-screen">
                        <div className="mock-ocr-container">
                          <div className="ocr-viewport">
                            <div className="ocr-laser-line"></div>
                            <div className="ocr-sample-text">
                              "DEPARTMENT OF COMPUTER ENGINEERING
                              <br />
                              NOTICE: MID-SEMESTER EXAM SEATING
                              <br />
                              ROLL NO: 2021UCO1642 &rarr; DESK 14"
                            </div>
                          </div>
                          <div className="ocr-result-pill">
                            ✓ Notice Extracted • Processed locally in 140ms
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* Tabs Switcher Below Phone */}
              <div className="phone-tabs">
                {[
                  { id: 'attendance', label: 'Attendance' },
                  { id: 'timetable', label: 'Timetable' },
                  { id: 'exams', label: 'Exam Hall' },
                  { id: 'ocr', label: 'OCR Scanner' },
                ].map((tab) => (
                  <button
                    key={tab.id}
                    className={`tab-btn ${activeTab === tab.id ? 'active' : ''}`}
                    onClick={() => setActiveTab(tab.id)}
                  >
                    {tab.label}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* Features Bento Grid */}
        <section className="features-section" id="features">
          <div className="section-header">
            <span className="section-tag">ENGINEERED FOR STUDENTS</span>
            <h2 className="section-title">
              Everything you need for college,
              <br />
              in one fluid interface.
            </h2>
            <p className="section-subtitle">
              Designed to replace clunky portal logins and slow web pages with
              instant, offline-first native performance.
            </p>
          </div>

          <div className="bento-grid">
            <div className="bento-card wide">
              <div className="card-icon-box">
                <ShieldCheck size={28} />
              </div>
              <h3>Zero CAPTCHA Friction with Session Persistence</h3>
              <p>
                Tired of typing security numbers every 5 minutes? NSync safely
                stores your authorized IMS session in the backend and sliding token
                cache. Log in once with official captcha verification, and navigate
                seamlessly for hours.
              </p>
            </div>

            <div className="bento-card">
              <div className="card-icon-box">
                <Percent size={28} />
              </div>
              <h3>Smart 75% Bunk Criteria</h3>
              <p>
                Never calculate manually on paper again. Live projections tell
                you exactly how many lectures you can safely skip or need to
                attend to stay safe from detentions.
              </p>
            </div>

            <div className="bento-card">
              <div className="card-icon-box">
                <LayoutGrid size={28} />
              </div>
              <h3>Home Screen Widgets</h3>
              <p>
                View your next class, room number, and attendance percentage
                without even opening the app. Tactile Glance widgets designed for
                your home screen.
              </p>
            </div>

            <div className="bento-card">
              <div className="card-icon-box">
                <WifiOff size={28} />
              </div>
              <h3>Offline Timetable Sync</h3>
              <p>
                Syncs your class schedule to local database cache. Checks your
                timetable even inside poor network lecture halls and basements.
              </p>
            </div>

            <div className="bento-card">
              <div className="card-icon-box">
                <Sparkles size={28} />
              </div>
              <h3>In-App Auto-Updates</h3>
              <p>
                Installed directly from GitHub? Whenever a new feature or fix is
                released, NSync alerts you and installs the new version in one tap.
              </p>
            </div>
          </div>
        </section>

        {/* Install Guide Section */}
        <section className="guide-section" id="install">
          <div className="section-header">
            <span className="section-tag">SIDELOADING GUIDE</span>
            <h2 className="section-title">Up and running in 3 simple steps</h2>
            <p className="section-subtitle">
              No Google Play account needed. Download and install directly onto
              any Android device running Android 7.0 or higher.
            </p>
          </div>

          <div className="guide-steps">
            <div className="step-card">
              <div className="step-num">01</div>
              <h4>Download the APK</h4>
              <p>
                Tap the download button to grab the official signed{' '}
                <code>NSync-latest.apk</code> directly from our verified GitHub
                Releases.
              </p>
            </div>

            <div className="step-card">
              <div className="step-num">02</div>
              <h4>Allow Unknown Apps</h4>
              <p>
                When prompted by Android, toggle "Allow from this source" in
                Settings. Our APK is 100% open-source and free of trackers.
              </p>
            </div>

            <div className="step-card">
              <div className="step-num">03</div>
              <h4>Log In & Auto-Sync</h4>
              <p>
                Log in once with your NSUT IMS credentials and official CAPTCHA.
                Your session is safely retained for smooth daily usage.
              </p>
            </div>
          </div>
        </section>

        {/* CTA Banner Section */}
        <section className="cta-banner-section">
          <div className="cta-banner">
            <h2>Ready to streamline your college routine?</h2>
            <p>
              Join NSUT students who never miss an attendance milestone, timetable
              change, or exam hall notification.
            </p>
            <a href={release.url} className="btn-download-primary" style={{ margin: '0 auto' }}>
              <div className="btn-inner">
                <Download size={22} />
                <span>Download NSync APK</span>
              </div>
              <span className="btn-meta">
                Android 7.0+ • ~{release.size} • Direct Release
              </span>
            </a>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer>
        <div className="footer-content">
          <div className="footer-credits">
            <p>
              <strong>NSync</strong> — Handcrafted for Netaji Subhas University of
              Technology (NSUT).
            </p>
            <p style={{ fontSize: '0.8rem', marginTop: '4px' }}>
              Open-source student companion app. Not officially affiliated with
              NSUT administration.
            </p>
          </div>

          <ul className="footer-links">
            <li>
              <a
                href="https://github.com/senkuthegreat/NSync"
                target="_blank"
                rel="noopener noreferrer"
              >
                GitHub Repository
              </a>
            </li>
            <li>
              <a
                href="https://github.com/senkuthegreat/NSync/releases"
                target="_blank"
                rel="noopener noreferrer"
              >
                Releases
              </a>
            </li>
            <li>
              <a
                href="https://github.com/senkuthegreat/NSync/issues"
                target="_blank"
                rel="noopener noreferrer"
              >
                Bug Reports
              </a>
            </li>
          </ul>
        </div>
      </footer>
    </div>
  );
}
