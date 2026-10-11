import React, { useState, useEffect } from 'react';
import {
  Download,
  ShieldCheck,
  Percent,
  LayoutGrid,
  WifiOff,
  Sparkles,
  BarChart2,
  Calendar,
  Clock,
  User,
  Key,
  Eye,
  ChevronRight,
  LogOut,
  Calculator,
  FileText
} from 'lucide-react';

// Direct package download hosted locally on the site CDN (Zero GitHub links exposed)
const LATEST_DOWNLOAD_URL = './downloads/NSync.apk';

// Dedicated high-resolution artwork: Pink Sunset Clouds (matching reference)
const HERO_ARTWORK = './assets/artworks/sunset-clouds.jpg';

export default function App() {
  const [activePhoneTab, setActivePhoneTab] = useState('attendance');
  const [activeDay, setActiveDay] = useState('Mon');
  const [scrolled, setScrolled] = useState(false);

  // Header scroll state
  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div className="page-wrapper">
      {/* Outer Floating Device Window Container (Matching Reference Aesthetic) */}
      <div className="canvas-frame">
        {/* Hero Section with Immersive Artwork Background */}
        <div
          className="hero-container"
          style={{ backgroundImage: `url(${HERO_ARTWORK})` }}
        >
          {/* Subtle Contrast Gradient Overlay for Typography Clarity */}
          <div className="hero-gradient-overlay"></div>

          {/* Top Clean Header (NSync Logo & Download Pill Only) */}
          <header className={`hero-nav ${scrolled ? 'nav-scrolled' : ''}`}>
            <a href="#" className="brand-group">
              <img
                src="./assets/logo.png"
                alt="NSync Logo"
                className="brand-logo"
              />
              <span className="brand-title">
                N<span>Sync</span>
              </span>
            </a>

            <div className="nav-actions">
              <a
                href={LATEST_DOWNLOAD_URL}
                download="NSync.apk"
                className="nav-btn-pill"
              >
                <Download size={15} />
                <span>Download</span>
              </a>
            </div>
          </header>

          {/* Hero Headlines & CTA */}
          <div className="hero-hero-content">
            <h1 className="hero-main-title">Academic Life, Seamlessly Synced.</h1>
            <p className="hero-main-subtitle">
              Attendance tracking with 75% criteria calculator, daily timetables,
              and exam hall seating — built exclusively for NSUT students.
            </p>

            <div className="hero-cta-wrapper">
              <a
                href={LATEST_DOWNLOAD_URL}
                download="NSync.apk"
                className="hero-get-started-btn"
              >
                <span>Download for Android</span>
                <Download size={17} />
              </a>
            </div>
          </div>

          {/* Interactive Mobile Device Stage (Flagship Phone + Live Flanking Widgets) */}
          <div id="showcase" className="hero-device-stage">
            {/* Interactive Screen Selector Tabs */}
            <div className="stage-tabs-pill">
              <button
                className={`stage-tab-btn ${activePhoneTab === 'attendance' ? 'active' : ''}`}
                onClick={() => setActivePhoneTab('attendance')}
              >
                <BarChart2 size={14} />
                <span>Attendance</span>
              </button>
              <button
                className={`stage-tab-btn ${activePhoneTab === 'schedule' ? 'active' : ''}`}
                onClick={() => setActivePhoneTab('schedule')}
              >
                <Calendar size={14} />
                <span>Schedule</span>
              </button>
              <button
                className={`stage-tab-btn ${activePhoneTab === 'more' ? 'active' : ''}`}
                onClick={() => setActivePhoneTab('more')}
              >
                <LayoutGrid size={14} />
                <span>More</span>
              </button>
              <button
                className={`stage-tab-btn ${activePhoneTab === 'login' ? 'active' : ''}`}
                onClick={() => setActivePhoneTab('login')}
              >
                <User size={14} />
                <span>Sign In</span>
              </button>
            </div>

            {/* Stage Centerpiece: Central Phone + Flanking Floating Glass Widgets */}
            <div className="stage-centerpiece">
              {/* Left Flanking Glass Card: Attendance Guard */}
              <div className="flanking-card left-card">
                <div className="flank-badge safe">
                  <Percent size={12} />
                  <span>75% Criteria Guard</span>
                </div>
                <div className="flank-stat">
                  <span className="flank-number">92.3%</span>
                  <span className="flank-tag safe">+6 Bunks Safe</span>
                </div>
                <p className="flank-desc">
                  <strong>Digital Circuits and Systems</strong> is safe above 75%. Live calculations show exact skips or needed classes for each subject.
                </p>
                <div className="flank-sub-chips">
                  <span className="chip danger">Physics: Need 26</span>
                  <span className="chip warning">Signals: Need 16</span>
                </div>
              </div>

              {/* Central Titanium Flagship Smartphone */}
              <div className="device-phone">
                <div className="device-screen">
                  {/* Status Bar with Dynamic Island */}
                  <div className="phone-status-bar">
                    <span>09:41</span>
                    <div className="phone-island"></div>
                    <span>5G • 100%</span>
                  </div>

                  {/* Dynamic Tab Screen Container */}
                  <div className="phone-screen-body">
                    {/* Screen 1: Attendance (Exact match with App Screen) */}
                    {activePhoneTab === 'attendance' && (
                      <div className="real-screen attendance-view fade-in">
                        <div className="screen-main-heading">Attendance</div>
                        <div className="att-cards-container">
                          <div className="att-subject-row">
                            <div className="att-row-left">
                              <h5>Solid State Physics</h5>
                              <span className="att-sub-tag danger">Need 26 Classes</span>
                            </div>
                            <span className="att-pct-badge danger">45.5%</span>
                          </div>

                          <div className="att-subject-row">
                            <div className="att-row-left">
                              <h5>Signals and Systems</h5>
                              <span className="att-sub-tag danger">Need 16 Classes</span>
                            </div>
                            <span className="att-pct-badge danger">58.3%</span>
                          </div>

                          <div className="att-subject-row">
                            <div className="att-row-left">
                              <h5>Microelectronics</h5>
                              <span className="att-sub-tag warning">Need 2 Classes</span>
                            </div>
                            <span className="att-pct-badge warning">72.2%</span>
                          </div>

                          <div className="att-subject-row">
                            <div className="att-row-left">
                              <h5>Probability Theory and Random Process</h5>
                              <span className="att-sub-tag warning">Need 3 Classes</span>
                            </div>
                            <span className="att-pct-badge warning">72.4%</span>
                          </div>

                          <div className="att-subject-row">
                            <div className="att-row-left">
                              <h5>Digital Circuits and Systems</h5>
                              <span className="att-sub-tag safe">+6 Bunks Safe</span>
                            </div>
                            <span className="att-pct-badge safe">92.3%</span>
                          </div>

                          <div className="att-subject-row">
                            <div className="att-row-left">
                              <h5>Community Internship</h5>
                              <span className="att-sub-tag neutral">No classes held yet</span>
                            </div>
                            <span className="att-pct-badge neutral">0 Held</span>
                          </div>

                          <div className="att-subject-row">
                            <div className="att-row-left">
                              <h5>Innovation</h5>
                              <span className="att-sub-tag neutral">No classes held yet</span>
                            </div>
                            <span className="att-pct-badge neutral">0 Held</span>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Screen 2: Schedule (Exact match with App Screen) */}
                    {activePhoneTab === 'schedule' && (
                      <div className="real-screen schedule-view fade-in">
                        <div className="sched-header-row">
                          <div className="screen-main-heading">Schedule</div>
                          <div className="sched-header-actions">
                            <div className="gcal-pill">
                              <span className="gcal-icon">📅</span>
                              <span>Google Cal</span>
                            </div>
                            <div className="cal-icon-btn">
                              <Calendar size={13} />
                            </div>
                          </div>
                        </div>

                        <div className="sched-day-selector">
                          {['Mon', 'Tue', 'Wed', 'Thu', 'Fri'].map((day) => (
                            <button
                              key={day}
                              className={`sched-day-btn ${activeDay === day ? 'active' : ''}`}
                              onClick={() => setActiveDay(day)}
                            >
                              {day}
                            </button>
                          ))}
                        </div>

                        <div className="sched-day-meta">
                          <span>{activeDay.toUpperCase()}DAY</span>
                          <span>4 classes</span>
                        </div>

                        <div className="sched-items-container">
                          <div className="sched-class-card">
                            <div className="class-card-text">
                              <h5>Digital Circuits and Systems</h5>
                              <span className="class-time">10:00 AM – 11:00 AM</span>
                            </div>
                            <span className="class-badge lecture">LECTURE</span>
                          </div>

                          <div className="sched-break-card">
                            <span>Break (1 hr)</span>
                            <span className="break-time">11:00 AM – 12:00 PM</span>
                          </div>

                          <div className="sched-class-card">
                            <div className="class-card-text">
                              <h5>Microelectronics</h5>
                              <span className="class-time">12:00 PM – 1:00 PM</span>
                            </div>
                            <span className="class-badge lecture">LECTURE</span>
                          </div>

                          <div className="sched-break-card">
                            <span>Break (1 hr)</span>
                            <span className="break-time">1:00 PM – 2:00 PM</span>
                          </div>

                          <div className="sched-class-card">
                            <div className="class-card-text">
                              <h5>Signals and Systems</h5>
                              <span className="class-time">2:00 PM – 4:00 PM</span>
                            </div>
                            <span className="class-badge tutorial">TUTORIAL</span>
                          </div>

                          <div className="sched-class-card">
                            <div className="class-card-text">
                              <h5>Probability Theory and Random Pro...</h5>
                              <span className="class-time">4:00 PM – 6:00 PM</span>
                            </div>
                            <span className="class-badge tutorial">TUTORIAL</span>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Screen 3: More (Exact match with App Screen) */}
                    {activePhoneTab === 'more' && (
                      <div className="real-screen more-view fade-in">
                        <div className="screen-main-heading">More</div>

                        <div className="student-profile-banner">
                          <div className="profile-avatar-magenta">
                            <div className="avatar-inner-ring">
                              <User size={18} color="#ffffff" />
                            </div>
                          </div>
                          <div className="profile-banner-info">
                            <h5>STUDENT</h5>
                            <span className="profile-tag-blue">UNKEMPT</span>
                            <p>Electronics And Communication Engineering</p>
                          </div>
                        </div>

                        <div className="more-menu-section-title">ACADEMIC & PERFORMANCE</div>
                        <div className="more-menu-group-card">
                          <div className="more-row-item">
                            <div className="more-row-left">
                              <div className="menu-icon-square">
                                <Calculator size={14} />
                              </div>
                              <span>Results & CGPA</span>
                            </div>
                            <ChevronRight size={14} className="row-chevron" />
                          </div>

                          <div className="more-row-item">
                            <div className="more-row-left">
                              <div className="menu-icon-square">
                                <Calendar size={14} />
                              </div>
                              <span>Academic Calendar</span>
                            </div>
                            <ChevronRight size={14} className="row-chevron" />
                          </div>

                          <div className="more-row-item">
                            <div className="more-row-left">
                              <div className="menu-icon-square">
                                <FileText size={14} />
                              </div>
                              <span>Exam Datesheet & Seating Plan</span>
                            </div>
                            <ChevronRight size={14} className="row-chevron" />
                          </div>
                        </div>

                        <div className="more-menu-section-title">HOME SCREEN & CUSTOMIZATION</div>
                        <div className="more-menu-group-card">
                          <div className="more-row-item">
                            <div className="more-row-left">
                              <div className="menu-icon-square">
                                <LayoutGrid size={14} />
                              </div>
                              <span>Widget Studio</span>
                            </div>
                            <ChevronRight size={14} className="row-chevron" />
                          </div>
                        </div>

                        <div className="logout-action-box">
                          <LogOut size={14} />
                          <span>Log Out of NSUT IMS</span>
                        </div>
                      </div>
                    )}

                    {/* Screen 4: Login (Exact match with App Screen) */}
                    {activePhoneTab === 'login' && (
                      <div className="real-screen login-view fade-in">
                        <div className="login-logo-header">
                          <h2 className="login-brand-unkempt">
                            N<span>Sync</span>
                          </h2>
                        </div>

                        <div className="login-form-card">
                          <div className="login-input-group">
                            <label>ROLL NUMBER / USER ID</label>
                            <div className="login-input-box">
                              <User size={15} className="input-icon" />
                              <span className="input-placeholder">e.g. 2024UCO1500</span>
                            </div>
                          </div>

                          <div className="login-input-group">
                            <label>IMS PASSWORD</label>
                            <div className="login-input-box">
                              <Key size={15} className="input-icon" />
                              <span className="input-placeholder">Enter password</span>
                              <Eye size={15} className="eye-icon" />
                            </div>
                          </div>
                        </div>

                        <button className="login-primary-btn">
                          Sign In
                        </button>
                      </div>
                    )}
                  </div>

                  {/* Authentic In-App Bottom Navigation Bar (Across Attendance, Schedule, More) */}
                  {activePhoneTab !== 'login' && (
                    <div className="real-bottom-nav">
                      <button
                        className={`bnav-item ${activePhoneTab === 'attendance' ? 'active' : ''}`}
                        onClick={() => setActivePhoneTab('attendance')}
                      >
                        <BarChart2 size={16} />
                        <span>Attendance</span>
                      </button>

                      <button
                        className={`bnav-item ${activePhoneTab === 'schedule' ? 'active' : ''}`}
                        onClick={() => setActivePhoneTab('schedule')}
                      >
                        <Calendar size={16} />
                        <span>Schedule</span>
                      </button>

                      <button
                        className={`bnav-item ${activePhoneTab === 'more' ? 'active' : ''}`}
                        onClick={() => setActivePhoneTab('more')}
                      >
                        <LayoutGrid size={16} />
                        <span>More</span>
                      </button>
                    </div>
                  )}
                </div>
              </div>

              {/* Right Flanking Glass Card: Today's Schedule */}
              <div className="flanking-card right-card">
                <div className="flank-badge schedule">
                  <Clock size={12} />
                  <span>Today's Schedule</span>
                </div>
                <div className="flank-stat">
                  <span className="flank-class-title">Digital Circuits & Systems</span>
                </div>
                <div className="flank-loc-row">
                  <Clock size={13} />
                  <span>10:00 AM – 11:00 AM • Lecture</span>
                </div>
                <div className="flank-countdown-pill">
                  <span className="pulse-dot"></span>
                  <span>Google Calendar Sync Ready</span>
                </div>
                <div className="flank-offline-note">
                  <LayoutGrid size={12} />
                  <span>Customizable via Widget Studio</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Section 2: Features Bento Grid (Clean Light SaaS Palette) */}
        <section id="features" className="content-section features-area">
          <div className="section-title-wrap">
            <span className="section-pill">Intelligence & Design</span>
            <h2>Engineered to eliminate friction.</h2>
            <p>
              Everything you need to navigate your college lifecycle with zero
              stress and zero captcha friction.
            </p>
          </div>

          <div className="features-grid">
            <div className="feature-box wide">
              <div className="icon-badge">
                <ShieldCheck size={24} />
              </div>
              <h3>Persistent IMS Sessions</h3>
              <p>
                Log in once with official credentials. Your session is safely
                cached locally on the device so you never have to re-enter
                passwords when refreshing attendance or schedule data.
              </p>
            </div>

            <div className="feature-box">
              <div className="icon-badge">
                <Percent size={24} />
              </div>
              <h3>Smart 75% Projections</h3>
              <p>
                Instantly calculates whether you are safe with <code>+X Bunks Safe</code> or in shortage with <code>Need X Classes</code> for every individual course.
              </p>
            </div>

            <div className="feature-box">
              <div className="icon-badge">
                <LayoutGrid size={24} />
              </div>
              <h3>Widget Studio</h3>
              <p>
                Add glanceable home screen widgets to your Android phone to track
                your next lecture hall and attendance without opening the app.
              </p>
            </div>

            <div className="feature-box">
              <div className="icon-badge">
                <WifiOff size={24} />
              </div>
              <h3>100% Offline Architecture</h3>
              <p>
                Full local database caching ensures your timetable and syllabus
                are available instantly, even in basements with zero cell reception.
              </p>
            </div>

            <div className="feature-box">
              <div className="icon-badge">
                <Sparkles size={24} />
              </div>
              <h3>Google Calendar Sync</h3>
              <p>
                Export your semester schedule to Google Calendar with a single tap
                for automated class notifications and agenda alerts.
              </p>
            </div>
          </div>
        </section>

        {/* Section 3: Setup Guide */}
        <section id="setup" className="content-section setup-area">
          <div className="section-title-wrap">
            <span className="section-pill">Installation</span>
            <h2>Up and running in moments.</h2>
            <p>Ready for any Android device running Android 7.0 or newer.</p>
          </div>

          <div className="setup-steps-row">
            <div className="setup-step-card">
              <div className="step-badge">01</div>
              <h4>Download Package</h4>
              <p>
                Download the official <code>NSync.apk</code> package directly
                from this website with one tap.
              </p>
            </div>

            <div className="setup-step-card">
              <div className="step-badge">02</div>
              <h4>Approve Installation</h4>
              <p>
                Open the file and allow install when prompted. Zero ads, zero
                third-party trackers.
              </p>
            </div>

            <div className="setup-step-card">
              <div className="step-badge">03</div>
              <h4>Instant Sync</h4>
              <p>
                Sign in once with your IMS credentials. Your attendance and
                timetable sync automatically.
              </p>
            </div>
          </div>
        </section>

        {/* Section 4: Final Download Banner */}
        <section className="cta-banner-wrapper">
          <div className="cta-banner-card">
            <h2>Experience NSync today.</h2>
            <p>
              Handcrafted exclusively for students of Netaji Subhas University of
              Technology.
            </p>
            <a
              href={LATEST_DOWNLOAD_URL}
              download="NSync.apk"
              className="hero-get-started-btn"
              style={{ margin: '0 auto' }}
            >
              <span>Download for Android</span>
              <Download size={18} />
            </a>
          </div>
        </section>

        {/* Clean, Professional Footer (Zero GitHub References) */}
        <footer className="canvas-footer">
          <div className="footer-left">
            <div className="brand-group">
              <img
                src="./assets/logo.png"
                alt="NSync Logo"
                className="brand-logo small"
              />
              <span className="brand-title">
                N<span>Sync</span>
              </span>
            </div>
            <p className="footer-disclaimer">
              Student companion for NSUT. Not officially affiliated with
              university administration.
            </p>
          </div>

          <div className="footer-right">
            <a href="#showcase">Showcase</a>
            <a href="#features">Features</a>
            <a href="#setup">Setup</a>
            <a href={LATEST_DOWNLOAD_URL} download="NSync.apk">
              Download APK
            </a>
          </div>
        </footer>
      </div>
    </div>
  );
}
