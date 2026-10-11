import React, { useState, useEffect, useRef } from 'react';
import {
  Download,
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
  BookOpen
} from 'lucide-react';

// Always points directly to the latest released APK on GitHub
const LATEST_DOWNLOAD_URL =
  'https://github.com/senkuthegreat/NSync/releases/latest/download/NSync-latest.apk';

export default function App() {
  const [scrolled, setScrolled] = useState(false);
  const [activeTab, setActiveTab] = useState('attendance');
  const [activeDay, setActiveDay] = useState('Wed');
  const [tilt, setTilt] = useState({ x: 0, y: 0 });

  const phoneRef = useRef(null);

  // Scroll listener for subtle header elevation
  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 15);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // 3D Parallax Tilt Handler
  const handleMouseMove = (e) => {
    if (!phoneRef.current) return;
    const rect = phoneRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    const centerX = rect.width / 2;
    const centerY = rect.height / 2;

    const tiltX = -((y - centerY) / centerY) * 10;
    const tiltY = ((x - centerX) / centerX) * 12;

    setTilt({ x: tiltX, y: tiltY });
  };

  const handleMouseLeave = () => {
    setTilt({ x: 0, y: 0 });
  };

  return (
    <div className="app-container">
      {/* Cinematic Ambient Lighting (Zero Grids, Pure Apple Obsidian Depth) */}
      <div className="bg-ambient">
        <div className="ambient-glow-center"></div>
      </div>

      {/* Header (Minimal, Big Logo, Unkempt Brand, Zero Extra Links, Zero Borders) */}
      <header className={scrolled ? 'scrolled' : ''}>
        <div className="nav-container">
          <a href="#" className="brand">
            <img src="./assets/logo.png" alt="NSync Logo" className="brand-logo" />
            <span className="brand-text">
              N<span>Sync</span>
            </span>
          </a>

          <a href={LATEST_DOWNLOAD_URL} className="nav-cta-btn">
            <Download size={16} />
            <span>Download</span>
          </a>
        </div>
      </header>

      {/* Main Content */}
      <main>
        {/* Hero Section */}
        <section className="hero-section">
          <div className="hero-grid">
            {/* Left Column: Confident, Editorial Copy */}
            <div className="hero-content">
              <h1 className="hero-title">
                Your campus.
                <br />
                <span className="gradient-text">In perfect sync.</span>
              </h1>

              <p className="hero-subtitle">
                Attendance tracking with smart bunk projections, live schedules,
                and exam hall tickets. Zero captchas, 100% offline. Designed
                exclusively for NSUT students.
              </p>

              <div className="hero-actions">
                <a href={LATEST_DOWNLOAD_URL} className="btn-download-primary">
                  <Download size={19} />
                  <span>Download for Android</span>
                </a>
              </div>
            </div>

            {/* Right Column: 3D Precision Phone Mockup */}
            <div className="phone-showcase">
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
                  {/* Dynamic Island */}
                  <div className="phone-island"></div>

                  <div className="screen-content">
                    {/* Status Bar */}
                    <div className="mock-status-bar">
                      <span>09:41</span>
                      <span>5G • 100%</span>
                    </div>

                    {/* App Header */}
                    <div className="mock-header">
                      <div className="mock-greeting">
                        <h4>Hey, Senku 👋</h4>
                        <p>COE • 6th Semester • NSUT</p>
                      </div>
                      <div className="mock-sync-badge">Synced</div>
                    </div>

                    {/* Screen View 1: Attendance */}
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
                          <p className="mock-advice">
                            You can safely skip <strong>4 lecture(s)</strong>{' '}
                            above 75%.
                          </p>
                        </div>

                        <div className="mock-subject-list">
                          <div className="mock-subject-item">
                            <div className="subject-header">
                              <span>Compiler Design</span>
                              <span style={{ color: '#a5b4fc' }}>91.2%</span>
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
                              <span style={{ color: '#a5b4fc' }}>85.7%</span>
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
                              <span style={{ color: '#f59e0b' }}>76.0%</span>
                            </div>
                            <div className="progress-bar-bg">
                              <div
                                className="progress-bar-fill"
                                style={{ width: '76%', background: '#f59e0b' }}
                              ></div>
                            </div>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Screen View 2: Timetable */}
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

                    {/* Screen View 3: Exams */}
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

                    {/* Screen View 4: OCR */}
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

              {/* Segmented Control Tabs */}
              <div className="phone-tabs">
                {[
                  { id: 'attendance', label: 'Attendance' },
                  { id: 'timetable', label: 'Timetable' },
                  { id: 'exams', label: 'Exams' },
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

        {/* Features Bento Grid (Apple-Grade Cleanliness) */}
        <section className="features-section">
          <div className="section-header">
            <span className="section-tag">Features</span>
            <h2 className="section-title">
              Intelligent by design.
            </h2>
            <p className="section-subtitle">
              Every interaction is engineered to remove friction from your
              academic routine.
            </p>
          </div>

          <div className="bento-grid">
            <div className="bento-card wide">
              <div className="card-icon-box">
                <ShieldCheck size={26} />
              </div>
              <h3>Persistent IMS Sessions</h3>
              <p>
                Log in once with official captcha verification. Your session is
                safely preserved on the backend so you never have to re-enter
                captchas when refreshing your data or moving between screens.
              </p>
            </div>

            <div className="bento-card">
              <div className="card-icon-box">
                <Percent size={26} />
              </div>
              <h3>Smart 75% Projections</h3>
              <p>
                Real-time safety calculations show exactly how many classes you
                can safely skip or need to attend to protect your eligibility.
              </p>
            </div>

            <div className="bento-card">
              <div className="card-icon-box">
                <LayoutGrid size={26} />
              </div>
              <h3>Glance Widgets</h3>
              <p>
                Check your next lecture hall and live attendance directly from
                your Android home screen without even launching the app.
              </p>
            </div>

            <div className="bento-card">
              <div className="card-icon-box">
                <WifiOff size={26} />
              </div>
              <h3>Offline Architecture</h3>
              <p>
                Full local database caching ensures your timetable and syllabus
                are available instantly, even in basements with zero cellular
                signal.
              </p>
            </div>

            <div className="bento-card">
              <div className="card-icon-box">
                <Sparkles size={26} />
              </div>
              <h3>Seamless Updates</h3>
              <p>
                Direct OTA updates notify you whenever a release is published
                and install upgrades with a single tap.
              </p>
            </div>
          </div>
        </section>

        {/* Install Guide Section */}
        <section className="guide-section">
          <div className="section-header">
            <span className="section-tag">Setup</span>
            <h2 className="section-title">Up and running in moments.</h2>
            <p className="section-subtitle">
              Free, open source, and ready for any Android device running Android
              7.0 or newer.
            </p>
          </div>

          <div className="guide-steps">
            <div className="step-card">
              <div className="step-num">01</div>
              <h4>Download</h4>
              <p>
                Download the official signed <code>NSync-latest.apk</code> directly
                from GitHub Releases.
              </p>
            </div>

            <div className="step-card">
              <div className="step-num">02</div>
              <h4>Install</h4>
              <p>
                Allow installation from your browser when prompted. 100%
                open-source with zero trackers.
              </p>
            </div>

            <div className="step-card">
              <div className="step-num">03</div>
              <h4>Sync</h4>
              <p>
                Authenticate once with your IMS portal credentials. Your timetable
                and attendance sync instantly.
              </p>
            </div>
          </div>
        </section>

        {/* CTA Banner Section */}
        <section className="cta-banner-section">
          <div className="cta-banner">
            <h2>Experience NSync.</h2>
            <p>
              Built specifically for students of Netaji Subhas University of
              Technology.
            </p>
            <a href={LATEST_DOWNLOAD_URL} className="btn-download-primary" style={{ margin: '0 auto' }}>
              <Download size={19} />
              <span>Download for Android</span>
            </a>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer>
        <div className="footer-content">
          <div className="footer-credits">
            <p>
              <strong>NSync</strong> — Handcrafted for NSUT.
            </p>
            <p style={{ fontSize: '0.8rem', marginTop: '4px', color: '#636366' }}>
              Open-source student companion. Not officially affiliated with
              university administration.
            </p>
          </div>

          <ul className="footer-links">
            <li>
              <a
                href="https://github.com/senkuthegreat/NSync"
                target="_blank"
                rel="noopener noreferrer"
              >
                GitHub
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
                Feedback
              </a>
            </li>
          </ul>
        </div>
      </footer>
    </div>
  );
}
