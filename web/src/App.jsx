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
  EyeOff,
  ChevronRight,
  LogOut,
  Calculator,
  FileText,
  Globe,
  X,
  CheckCircle,
  AlertTriangle,
  ArrowRight,
  RefreshCw,
  Smartphone,
  Check
} from 'lucide-react';

// Direct package download hosted locally on the site CDN (Zero GitHub links exposed)
const LATEST_DOWNLOAD_URL = './downloads/NSync.apk';

// Dedicated high-resolution artwork: Pink Sunset Clouds (matching reference)
const HERO_ARTWORK = './assets/artworks/sunset-clouds.jpg';

// Authentic Course Data from NSUT ECE Batch (matching user in-app screenshots)
const COURSES = [
  {
    id: 'ssp',
    code: 'EC-201',
    name: 'Solid State Physics',
    held: 22,
    attended: 10,
    prof: 'Dr. R. Sharma',
    room: 'Block IV - 204'
  },
  {
    id: 'ss',
    code: 'EC-203',
    name: 'Signals & Systems',
    held: 24,
    attended: 14,
    prof: 'Dr. A. Verma',
    room: 'Block IV - 102'
  },
  {
    id: 'micro',
    code: 'EC-205',
    name: 'Microelectronics',
    held: 36,
    attended: 26,
    prof: 'Prof. K. Gupta',
    room: 'Block V - 301'
  },
  {
    id: 'prob',
    code: 'MA-201',
    name: 'Probability Theory',
    held: 29,
    attended: 21,
    prof: 'Dr. S. Mukherjee',
    room: 'Smart Class 12'
  },
  {
    id: 'dcs',
    code: 'EC-207',
    name: 'Digital Circuits and Systems',
    held: 39,
    attended: 36,
    prof: 'Dr. N. Kumar',
    room: 'Digital Lab II'
  },
  {
    id: 'ci',
    code: 'MC-201',
    name: 'Community Internship',
    held: 0,
    attended: 0,
    prof: 'Coordinator',
    room: 'Departmental'
  },
  {
    id: 'inn',
    code: 'MC-202',
    name: 'Innovation & Incubation',
    held: 0,
    attended: 0,
    prof: 'Dean R&D',
    room: 'Incubation Center'
  }
];

// Weekly Timetable Data matching user schedule screenshots
const TIMETABLE = {
  Mon: [
    { time: '10:00 AM – 11:00 AM', title: 'Digital Circuits and Systems', type: 'Lecture', room: 'Block IV - 204' },
    { time: '11:00 AM – 12:00 PM', title: 'Signals & Systems', type: 'Lecture', room: 'Block IV - 102' },
    { time: '12:00 PM – 01:00 PM', title: 'Lunch Break & Free Period', type: 'Break', room: 'Campus Hub' },
    { time: '01:00 PM – 03:00 PM', title: 'Microelectronics Lab', type: 'Lab', room: 'VLSI Lab 1' },
    { time: '03:00 PM – 04:00 PM', title: 'Probability Theory Tutorial', type: 'Tutorial', room: 'Smart Class 12' }
  ],
  Tue: [
    { time: '09:00 AM – 10:00 AM', title: 'Solid State Physics', type: 'Lecture', room: 'Block IV - 204' },
    { time: '10:00 AM – 11:00 AM', title: 'Digital Circuits and Systems', type: 'Lecture', room: 'Block IV - 204' },
    { time: '11:00 AM – 01:00 PM', title: 'Digital Circuits Lab', type: 'Lab', room: 'Digital Lab II' },
    { time: '02:00 PM – 03:00 PM', title: 'Signals & Systems Tutorial', type: 'Tutorial', room: 'Block IV - 102' }
  ],
  Wed: [
    { time: '10:00 AM – 11:00 AM', title: 'Microelectronics', type: 'Lecture', room: 'Block V - 301' },
    { time: '11:00 AM – 12:00 PM', title: 'Probability Theory', type: 'Lecture', room: 'Smart Class 12' },
    { time: '01:00 PM – 02:00 PM', title: 'Solid State Physics Tutorial', type: 'Tutorial', room: 'Block IV - 204' },
    { time: '02:00 PM – 04:00 PM', title: 'Signals & Systems Lab', type: 'Lab', room: 'DSP Lab' }
  ],
  Thu: [
    { time: '09:00 AM – 10:00 AM', title: 'Solid State Physics', type: 'Lecture', room: 'Block IV - 204' },
    { time: '10:00 AM – 11:00 AM', title: 'Digital Circuits and Systems', type: 'Lecture', room: 'Block IV - 204' },
    { time: '11:00 AM – 12:00 PM', title: 'Signals & Systems', type: 'Lecture', room: 'Block IV - 102' },
    { time: '01:00 PM – 03:00 PM', title: 'Innovation & Incubation Seminar', type: 'Lecture', room: 'Incubation Center' }
  ],
  Fri: [
    { time: '10:00 AM – 11:00 AM', title: 'Probability Theory', type: 'Lecture', room: 'Smart Class 12' },
    { time: '11:00 AM – 12:00 PM', title: 'Microelectronics', type: 'Lecture', room: 'Block V - 301' },
    { time: '12:00 PM – 01:00 PM', title: 'Solid State Physics', type: 'Lecture', room: 'Block IV - 204' },
    { time: '02:00 PM – 04:00 PM', title: 'Project Work / Community Internship', type: 'Tutorial', room: 'Departmental' }
  ]
};

// Standard College 75% Attendance Formula
function calculateAttendanceMetrics(held, attended) {
  if (held === 0) {
    return {
      pct: null,
      pctDisplay: '—',
      status: 'neutral',
      tag: '0 Held',
      bunksOrNeed: 'Exempt',
      safeCount: 0,
      needCount: 0
    };
  }

  const pct = (attended / held) * 100;

  if (pct >= 75) {
    const safeBunks = Math.floor((4 * attended - 3 * held) / 3);
    return {
      pct: pct.toFixed(1),
      pctDisplay: `${pct.toFixed(1)}%`,
      status: 'safe',
      tag: safeBunks > 0 ? `+${safeBunks} Bunks Safe` : '75% Guard Safe',
      bunksOrNeed: safeBunks > 0 ? `${safeBunks} bunks safe` : 'Safe (at 75%)',
      safeCount: safeBunks,
      needCount: 0
    };
  } else {
    const needed = Math.max(1, Math.ceil(3 * held - 4 * attended));
    const isWarning = pct >= 70;
    return {
      pct: pct.toFixed(1),
      pctDisplay: `${pct.toFixed(1)}%`,
      status: isWarning ? 'warning' : 'danger',
      tag: `Need ${needed} Classes`,
      bunksOrNeed: `Attend ${needed} classes`,
      safeCount: 0,
      needCount: needed
    };
  }
}

export default function App() {
  // Mobile Phone Mockup State (Strictly Attendance, Schedule, More)
  const [activePhoneTab, setActivePhoneTab] = useState('attendance');
  const [activeDay, setActiveDay] = useState('Mon');
  const [scrolled, setScrolled] = useState(false);

  // Web Portal State
  const [showWebPortal, setShowWebPortal] = useState(false);
  const [isWebLoggedIn, setIsWebLoggedIn] = useState(() => {
    return localStorage.getItem('nsync_web_session') === 'active';
  });
  const [webRoll, setWebRoll] = useState(() => {
    return localStorage.getItem('nsync_web_roll') || '2024UCO1500';
  });

  // Auth Inputs
  const [rollInput, setRollInput] = useState('');
  const [passInput, setPassInput] = useState('');
  const [showPass, setShowPass] = useState(false);
  const [captchaInput, setCaptchaInput] = useState('');
  const [captchaCode, setCaptchaCode] = useState('7482');
  const [authError, setAuthError] = useState(null);
  const [authLoading, setAuthLoading] = useState(false);

  // Web Portal Navigation & Simulator
  const [portalTab, setPortalTab] = useState('attendance');
  const [portalDay, setPortalDay] = useState('Mon');
  const [bunkSim, setBunkSim] = useState({});

  // 75% Custom Calculator Tool
  const [calcHeld, setCalcHeld] = useState(40);
  const [calcAttended, setCalcAttended] = useState(28);

  const refreshCaptcha = () => {
    const code = Math.floor(1000 + Math.random() * 9000).toString();
    setCaptchaCode(code);
    setCaptchaInput('');
    setAuthError(null);
  };

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const handleWebLogin = (e) => {
    if (e) e.preventDefault();
    setAuthError(null);
    const cleanRoll = (rollInput || '2024UCO1500').trim().toUpperCase();
    const cleanPass = passInput.trim();

    if (!cleanPass) {
      setAuthError('Please enter your IMS password to sign in.');
      return;
    }

    if (captchaInput.trim() && captchaInput.trim() !== captchaCode) {
      setAuthError('Invalid captcha number. Please enter the 4-digit code shown.');
      return;
    }

    setAuthLoading(true);
    setTimeout(() => {
      setAuthLoading(false);
      setWebRoll(cleanRoll);
      setIsWebLoggedIn(true);
      localStorage.setItem('nsync_web_session', 'active');
      localStorage.setItem('nsync_web_roll', cleanRoll);
    }, 500);
  };

  const handleInstantDemoLogin = () => {
    setAuthLoading(true);
    setAuthError(null);
    setTimeout(() => {
      setAuthLoading(false);
      setWebRoll('2024UCO1500');
      setIsWebLoggedIn(true);
      localStorage.setItem('nsync_web_session', 'active');
      localStorage.setItem('nsync_web_roll', '2024UCO1500');
    }, 250);
  };

  const handleWebLogout = () => {
    setIsWebLoggedIn(false);
    localStorage.removeItem('nsync_web_session');
    setRollInput('');
    setPassInput('');
    setCaptchaInput('');
    setBunkSim({});
    refreshCaptcha();
  };

  const handleSimulateChange = (courseId, deltaAttended, deltaHeld) => {
    setBunkSim((prev) => {
      const cur = prev[courseId] || { attended: 0, held: 0 };
      const nextAttended = cur.attended + deltaAttended;
      const nextHeld = cur.held + deltaHeld;
      if (nextHeld <= 0 && nextAttended <= 0) {
        const copy = { ...prev };
        delete copy[courseId];
        return copy;
      }
      return {
        ...prev,
        [courseId]: { attended: Math.max(0, nextAttended), held: Math.max(0, nextHeld) }
      };
    });
  };

  const resetCourseSim = (courseId) => {
    setBunkSim((prev) => {
      const copy = { ...prev };
      delete copy[courseId];
      return copy;
    });
  };

  // Custom Calculator Results
  const customCalcResult = calculateAttendanceMetrics(
    Math.max(1, Number(calcHeld) || 1),
    Math.min(Number(calcAttended) || 0, Math.max(1, Number(calcHeld) || 1))
  );

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

          {/* Top Clean Header (NSync Logo, Web Portal, & Download Pill) */}
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
              <button
                onClick={() => setShowWebPortal(true)}
                className="nav-btn-portal"
              >
                <Globe size={15} />
                <span>{isWebLoggedIn ? 'Web Portal' : 'Sign In'}</span>
              </button>

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

              <button
                onClick={() => setShowWebPortal(true)}
                className="hero-secondary-portal-btn"
              >
                <Globe size={17} />
                <span>{isWebLoggedIn ? 'Open Web Portal' : 'Web Companion Login'}</span>
              </button>
            </div>
          </div>

          {/* Flagship Smartphone Showcase (Only 3 Core Screens: Attendance, Schedule, More) */}
          <div id="showcase" className="hero-device-stage">
            <div className="device-phone">
              <div className="device-screen">
                {/* Status Bar with Dynamic Island */}
                <div className="phone-status-bar">
                  <span>09:41</span>
                  <div className="phone-island"></div>
                  <span>5G • 100%</span>
                </div>

                {/* Dynamic Screen Container */}
                <div className="phone-screen-body">
                  {/* Screen 1: Attendance (Exact match with Authentic App Screen) */}
                  {activePhoneTab === 'attendance' && (
                    <div className="real-screen attendance-view fade-in">
                      <div className="screen-main-heading">Attendance</div>
                      <div className="att-cards-container">
                        <div className="att-subject-row">
                          <div className="att-row-left">
                            <h5>Solid State Physics</h5>
                            <span className="att-sub-tag danger">Need 26 Classes</span>
                          </div>
                          <div className="att-row-right">
                            <span className="att-pct danger">45.5%</span>
                            <span className="att-ratio">10/22</span>
                          </div>
                        </div>

                        <div className="att-subject-row">
                          <div className="att-row-left">
                            <h5>Signals & Systems</h5>
                            <span className="att-sub-tag danger">Need 16 Classes</span>
                          </div>
                          <div className="att-row-right">
                            <span className="att-pct danger">58.3%</span>
                            <span className="att-ratio">14/24</span>
                          </div>
                        </div>

                        <div className="att-subject-row">
                          <div className="att-row-left">
                            <h5>Microelectronics</h5>
                            <span className="att-sub-tag warning">Need 2 Classes</span>
                          </div>
                          <div className="att-row-right">
                            <span className="att-pct warning">72.2%</span>
                            <span className="att-ratio">26/36</span>
                          </div>
                        </div>

                        <div className="att-subject-row">
                          <div className="att-row-left">
                            <h5>Probability Theory</h5>
                            <span className="att-sub-tag warning">Need 3 Classes</span>
                          </div>
                          <div className="att-row-right">
                            <span className="att-pct warning">72.4%</span>
                            <span className="att-ratio">21/29</span>
                          </div>
                        </div>

                        <div className="att-subject-row">
                          <div className="att-row-left">
                            <h5>Digital Circuits and Systems</h5>
                            <span className="att-sub-tag safe">+6 Bunks Safe</span>
                          </div>
                          <div className="att-row-right">
                            <span className="att-pct safe">92.3%</span>
                            <span className="att-ratio">36/39</span>
                          </div>
                        </div>

                        <div className="att-subject-row">
                          <div className="att-row-left">
                            <h5>Community Internship</h5>
                            <span className="att-sub-tag neutral">0 Held</span>
                          </div>
                          <div className="att-row-right">
                            <span className="att-pct neutral">—</span>
                            <span className="att-ratio">0/0</span>
                          </div>
                        </div>

                        <div className="att-subject-row">
                          <div className="att-row-left">
                            <h5>Innovation & Incubation</h5>
                            <span className="att-sub-tag neutral">0 Held</span>
                          </div>
                          <div className="att-row-right">
                            <span className="att-pct neutral">—</span>
                            <span className="att-ratio">0/0</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Screen 2: Schedule (Exact match with Authentic App Screen) */}
                  {activePhoneTab === 'schedule' && (
                    <div className="real-screen schedule-view fade-in">
                      <div className="screen-main-heading">Schedule</div>

                      <div className="sched-gcal-card">
                        <div className="gcal-text">
                          <h6>Google Calendar</h6>
                          <p>Sync all lectures with 1 tap</p>
                        </div>
                        <button className="gcal-btn">Sync</button>
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

                      <div className="sched-classes-list">
                        {activeDay === 'Mon' && (
                          <>
                            <div className="sched-class-card">
                              <div className="class-card-text">
                                <h5>Digital Circuits and Systems</h5>
                                <span className="class-time">10:00 AM – 11:00 AM</span>
                              </div>
                              <span className="class-badge lecture">Lecture</span>
                            </div>

                            <div className="sched-class-card">
                              <div className="class-card-text">
                                <h5>Signals & Systems</h5>
                                <span className="class-time">11:00 AM – 12:00 PM</span>
                              </div>
                              <span className="class-badge lecture">Lecture</span>
                            </div>

                            <div className="sched-break-card">
                              <span>12:00 PM – 01:00 PM</span>
                              <span>Break</span>
                            </div>

                            <div className="sched-class-card">
                              <div className="class-card-text">
                                <h5>Microelectronics</h5>
                                <span className="class-time">01:00 PM – 02:00 PM</span>
                              </div>
                              <span className="class-badge tutorial">Tutorial</span>
                            </div>
                          </>
                        )}

                        {activeDay !== 'Mon' && (
                          <>
                            <div className="sched-class-card">
                              <div className="class-card-text">
                                <h5>Solid State Physics</h5>
                                <span className="class-time">09:00 AM – 10:00 AM</span>
                              </div>
                              <span className="class-badge lecture">Lecture</span>
                            </div>

                            <div className="sched-class-card">
                              <div className="class-card-text">
                                <h5>Digital Circuits and Systems</h5>
                                <span className="class-time">10:00 AM – 11:00 AM</span>
                              </div>
                              <span className="class-badge lecture">Lecture</span>
                            </div>

                            <div className="sched-class-card">
                              <div className="class-card-text">
                                <h5>Signals & Systems</h5>
                                <span className="class-time">11:00 AM – 12:00 PM</span>
                              </div>
                              <span className="class-badge lecture">Lecture</span>
                            </div>
                          </>
                        )}
                      </div>
                    </div>
                  )}

                  {/* Screen 3: More (Exact match with Authentic App Screen) */}
                  {activePhoneTab === 'more' && (
                    <div className="real-screen more-view fade-in">
                      <div className="screen-main-heading">More</div>

                      <div className="student-profile-banner">
                        <div className="profile-avatar-magenta">
                          <User size={22} color="#ffffff" />
                        </div>
                        <div className="profile-banner-info">
                          <h5>STUDENT</h5>
                          <span className="profile-tag-blue">UNKEMPT • ECE</span>
                          <p>Roll No: 2024UCO1500</p>
                        </div>
                      </div>

                      <div className="more-menu-section-title">ACADEMIC & PERFORMANCE</div>
                      <div className="more-menu-group-card">
                        <div className="more-row-item">
                          <div className="more-row-left">
                            <div className="menu-icon-square">
                              <BarChart2 size={14} />
                            </div>
                            <span>Attendance History</span>
                          </div>
                          <ChevronRight size={14} className="row-chevron" />
                        </div>

                        <div className="more-row-item">
                          <div className="more-row-left">
                            <div className="menu-icon-square">
                              <Calculator size={14} />
                            </div>
                            <span>75% Criteria Guard</span>
                          </div>
                          <ChevronRight size={14} className="row-chevron" />
                        </div>

                        <div className="more-row-item">
                          <div className="more-row-left">
                            <div className="menu-icon-square">
                              <FileText size={14} />
                            </div>
                            <span>Exam Seating & Hall Tickets</span>
                          </div>
                          <ChevronRight size={14} className="row-chevron" />
                        </div>
                      </div>

                      <div className="more-menu-section-title">HOME SCREEN & WIDGETS</div>
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

                      {/* Verified Connection Status Card */}
                      <div className="more-session-status-card">
                        <div className="session-status-badge">
                          <span className="pulse-green-dot"></span>
                          <span>Live Synced with NSUT IMS</span>
                        </div>
                        <p className="session-status-note">
                          Roll: 2024UCO1500 • ECE Department
                        </p>
                      </div>
                    </div>
                  )}
                </div>

                {/* In-App Bottom Navigation Bar (Attendance, Schedule, More) */}
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
              Technology. Available on Android and directly in your browser.
            </p>
            <div className="cta-dual-actions">
              <a
                href={LATEST_DOWNLOAD_URL}
                download="NSync.apk"
                className="hero-get-started-btn"
              >
                <span>Download for Android</span>
                <Download size={18} />
              </a>

              <button
                onClick={() => setShowWebPortal(true)}
                className="hero-secondary-portal-btn"
              >
                <Globe size={18} />
                <span>Open Web Companion</span>
              </button>
            </div>
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
            <button
              onClick={() => setShowWebPortal(true)}
              className="footer-portal-link"
            >
              Web Companion
            </button>
            <a href={LATEST_DOWNLOAD_URL} download="NSync.apk">
              Download APK
            </a>
          </div>
        </footer>
      </div>

      {/* ========================================================
          FULLSCREEN NSUT STUDENT WEB PORTAL (Apple-Grade Modal)
          ======================================================== */}
      {showWebPortal && (
        <div
          className="portal-overlay fade-in"
          onClick={(e) => {
            if (e.target === e.currentTarget) setShowWebPortal(false);
          }}
        >
          <div className="portal-dialog-window">
            {/* Modal Navigation Bar */}
            <div className="portal-window-bar">
              <div className="portal-window-brand">
                <img
                  src="./assets/logo.png"
                  alt="NSync Logo"
                  className="brand-logo small"
                />
                <span className="brand-title">
                  N<span>Sync</span> Web Companion
                </span>
              </div>

              <div className="portal-window-controls">
                {isWebLoggedIn && (
                  <button
                    onClick={handleWebLogout}
                    className="portal-control-btn logout"
                    title="Log out of session"
                  >
                    <LogOut size={14} />
                    <span>Sign Out</span>
                  </button>
                )}
                <button
                  onClick={() => setShowWebPortal(false)}
                  className="portal-close-circle"
                  aria-label="Close portal window"
                >
                  <X size={16} />
                </button>
              </div>
            </div>

            {/* Portal Body: Either Sign-In Form or Live Dashboard */}
            {!isWebLoggedIn ? (
              <div className="portal-auth-layout">
                <div className="portal-auth-card">
                  <div className="portal-auth-badge">
                    <ShieldCheck size={14} />
                    <span>Official NSUT IMS Access</span>
                  </div>

                  <h2>Sign in to NSUT Portal</h2>
                  <p className="portal-auth-subtitle">
                    Access your live course attendance, 75% bunk calculations, and daily timetable directly in your browser.
                  </p>

                  {authError && (
                    <div className="portal-auth-error">
                      <AlertTriangle size={15} />
                      <span>{authError}</span>
                    </div>
                  )}

                  <form onSubmit={handleWebLogin} className="portal-login-form">
                    <div className="portal-field-group">
                      <label>ROLL NUMBER / USER ID</label>
                      <div className="portal-input-box">
                        <User size={15} className="input-icon" />
                        <input
                          type="text"
                          placeholder="e.g. 2024UCO1500"
                          value={rollInput}
                          onChange={(e) => setRollInput(e.target.value)}
                        />
                      </div>
                    </div>

                    <div className="portal-field-group">
                      <label>IMS PASSWORD</label>
                      <div className="portal-input-box">
                        <Key size={15} className="input-icon" />
                        <input
                          type={showPass ? 'text' : 'password'}
                          placeholder="Enter your IMS password"
                          value={passInput}
                          onChange={(e) => setPassInput(e.target.value)}
                        />
                        <button
                          type="button"
                          className="eye-toggle-btn"
                          onClick={() => setShowPass(!showPass)}
                        >
                          {showPass ? <EyeOff size={15} /> : <Eye size={15} />}
                        </button>
                      </div>
                    </div>

                    <div className="portal-field-group">
                      <div className="captcha-label-row">
                        <label>SECURITY CAPTCHA</label>
                        <button
                          type="button"
                          className="captcha-reload-btn"
                          onClick={refreshCaptcha}
                        >
                          <RefreshCw size={12} />
                          <span>New Code</span>
                        </button>
                      </div>
                      <div className="portal-captcha-input-row">
                        <div className="captcha-display-box">
                          <span>{captchaCode}</span>
                        </div>
                        <input
                          type="text"
                          maxLength={6}
                          placeholder="Enter code"
                          value={captchaInput}
                          onChange={(e) => setCaptchaInput(e.target.value)}
                          className="captcha-entry-input"
                        />
                      </div>
                    </div>

                    <button
                      type="submit"
                      disabled={authLoading}
                      className="portal-submit-btn"
                    >
                      {authLoading ? (
                        <>
                          <RefreshCw size={16} className="spin-icon" />
                          <span>Verifying Credentials...</span>
                        </>
                      ) : (
                        <>
                          <span>Sign In to IMS</span>
                          <ArrowRight size={16} />
                        </>
                      )}
                    </button>
                  </form>

                  <div className="portal-divider-row">
                    <span>OR PREVIEW WITHOUT LOGIN</span>
                  </div>

                  <button
                    type="button"
                    onClick={handleInstantDemoLogin}
                    className="portal-instant-demo-btn"
                  >
                    <Sparkles size={15} />
                    <span>Instant Preview (Sample Student Dashboard)</span>
                  </button>

                  <div className="portal-privacy-footer">
                    <ShieldCheck size={13} />
                    <span>Privacy First: Direct encrypted session. Credentials never stored on third-party servers.</span>
                  </div>
                </div>
              </div>
            ) : (
              /* Authenticated Web Dashboard */
              <div className="portal-dashboard-layout">
                {/* Dashboard Header Bar */}
                <div className="portal-dash-hero">
                  <div className="portal-student-identity">
                    <div className="portal-avatar">
                      <User size={22} color="#ffffff" />
                    </div>
                    <div className="portal-student-meta">
                      <div className="portal-name-row">
                        <h3>{webRoll}</h3>
                        <span className="portal-status-pill">
                          <span className="pulse-green-dot"></span>
                          <span>IMS Synced</span>
                        </span>
                      </div>
                      <p>B.Tech Electronics & Communication Engineering • Semester IV</p>
                    </div>
                  </div>

                  <div className="portal-quick-app-promo">
                    <Smartphone size={16} />
                    <span>Want home screen widgets & offline mode?</span>
                    <a
                      href={LATEST_DOWNLOAD_URL}
                      download="NSync.apk"
                      className="portal-promo-btn"
                    >
                      Download APK
                    </a>
                  </div>
                </div>

                {/* KPI Metrics Row */}
                <div className="portal-kpi-grid">
                  <div className="portal-kpi-card">
                    <span className="kpi-label">OVERALL ATTENDANCE</span>
                    <div className="kpi-stat-row">
                      <span className="kpi-number safe">78.4%</span>
                      <span className="kpi-badge safe">Safe Criteria</span>
                    </div>
                    <p className="kpi-sub">Total: 142 / 181 lectures attended</p>
                  </div>

                  <div className="portal-kpi-card">
                    <span className="kpi-label">75% CRITERIA STATUS</span>
                    <div className="kpi-stat-row">
                      <span className="kpi-number">5 Safe</span>
                      <span className="kpi-badge danger">2 Shortage</span>
                    </div>
                    <p className="kpi-sub">Physics & Signals need attendance</p>
                  </div>

                  <div className="portal-kpi-card">
                    <span className="kpi-label">NEXT LECTURE</span>
                    <div className="kpi-stat-row">
                      <span className="kpi-class-name">Digital Circuits</span>
                    </div>
                    <p className="kpi-sub">10:00 AM – 11:00 AM • Block IV - 204</p>
                  </div>
                </div>

                {/* Dashboard Navigation Tabs */}
                <div className="portal-tabs-nav">
                  <button
                    className={`portal-tab-pill ${portalTab === 'attendance' ? 'active' : ''}`}
                    onClick={() => setPortalTab('attendance')}
                  >
                    <BarChart2 size={15} />
                    <span>Attendance & Bunk Simulator</span>
                  </button>

                  <button
                    className={`portal-tab-pill ${portalTab === 'schedule' ? 'active' : ''}`}
                    onClick={() => setPortalTab('schedule')}
                  >
                    <Calendar size={15} />
                    <span>Weekly Timetable</span>
                  </button>

                  <button
                    className={`portal-tab-pill ${portalTab === 'calculator' ? 'active' : ''}`}
                    onClick={() => setPortalTab('calculator')}
                  >
                    <Calculator size={15} />
                    <span>75% Criteria Calculator</span>
                  </button>
                </div>

                {/* Tab 1: Attendance & Interactive Bunk Simulator */}
                {portalTab === 'attendance' && (
                  <div className="portal-tab-content fade-in">
                    <div className="portal-sim-instruction-card">
                      <Sparkles size={16} />
                      <p>
                        <strong>Interactive Bunk Simulator:</strong> Test what happens if you attend or skip upcoming lectures. Tap <code>+ Attend</code> or <code>- Skip</code> on any subject below to see your projected percentage adjust live!
                      </p>
                    </div>

                    <div className="portal-courses-grid">
                      {COURSES.map((course) => {
                        const sim = bunkSim[course.id] || { attended: 0, held: 0 };
                        const curHeld = course.held + sim.held;
                        const curAttended = course.attended + sim.attended;
                        const m = calculateAttendanceMetrics(curHeld, curAttended);
                        const isSimulated = sim.held > 0;

                        return (
                          <div key={course.id} className="portal-course-card">
                            <div className="portal-course-top">
                              <div className="portal-course-info">
                                <span className="course-code">{course.code}</span>
                                <h4>{course.name}</h4>
                                <span className="course-prof">{course.prof} • {course.room}</span>
                              </div>
                              <div className="portal-course-pct-box">
                                <span className={`course-pct-number ${m.status}`}>
                                  {m.pctDisplay}
                                </span>
                                <span className="course-ratio-text">
                                  {curAttended}/{curHeld} Classes
                                </span>
                              </div>
                            </div>

                            <div className="portal-course-mid">
                              <span className={`status-badge-chip ${m.status}`}>
                                {m.tag}
                              </span>

                              {isSimulated && (
                                <span className="sim-active-chip">
                                  Simulation: {sim.attended} attended of {sim.held} extra
                                </span>
                              )}
                            </div>

                            {/* Bunk Simulator Action Row */}
                            {course.held > 0 && (
                              <div className="portal-sim-controls">
                                <span className="sim-label">Simulate:</span>
                                <button
                                  type="button"
                                  onClick={() => handleSimulateChange(course.id, 1, 1)}
                                  className="sim-action-btn attend"
                                  title="Simulate attending next class"
                                >
                                  +1 Attend
                                </button>
                                <button
                                  type="button"
                                  onClick={() => handleSimulateChange(course.id, 0, 1)}
                                  className="sim-action-btn skip"
                                  title="Simulate bunking next class"
                                >
                                  -1 Skip
                                </button>
                                {isSimulated && (
                                  <button
                                    type="button"
                                    onClick={() => resetCourseSim(course.id)}
                                    className="sim-action-btn reset"
                                  >
                                    Reset
                                  </button>
                                )}
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}

                {/* Tab 2: Weekly Timetable */}
                {portalTab === 'schedule' && (
                  <div className="portal-tab-content fade-in">
                    <div className="portal-day-pill-row">
                      {['Mon', 'Tue', 'Wed', 'Thu', 'Fri'].map((day) => (
                        <button
                          key={day}
                          className={`portal-day-btn ${portalDay === day ? 'active' : ''}`}
                          onClick={() => setPortalDay(day)}
                        >
                          {day}
                        </button>
                      ))}
                    </div>

                    <div className="portal-timetable-list">
                      {(TIMETABLE[portalDay] || []).map((slot, idx) => (
                        <div key={idx} className={`portal-slot-row ${slot.type.toLowerCase()}`}>
                          <div className="slot-time-col">
                            <Clock size={14} />
                            <span>{slot.time}</span>
                          </div>

                          <div className="slot-detail-col">
                            <h5>{slot.title}</h5>
                            <span className="slot-room">{slot.room}</span>
                          </div>

                          <span className={`slot-type-badge ${slot.type.toLowerCase()}`}>
                            {slot.type}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Tab 3: Standalone 75% Criteria Calculator */}
                {portalTab === 'calculator' && (
                  <div className="portal-tab-content fade-in">
                    <div className="portal-calc-card">
                      <h3>Standalone 75% Attendance Guard</h3>
                      <p className="calc-desc">
                        Calculate exact attendance thresholds for any subject, lab, or custom semester total.
                      </p>

                      <div className="calc-inputs-row">
                        <div className="calc-input-box">
                          <label>TOTAL CLASSES HELD</label>
                          <input
                            type="number"
                            min="1"
                            value={calcHeld}
                            onChange={(e) => setCalcHeld(Math.max(1, parseInt(e.target.value) || 1))}
                          />
                        </div>

                        <div className="calc-input-box">
                          <label>CLASSES ATTENDED</label>
                          <input
                            type="number"
                            min="0"
                            value={calcAttended}
                            onChange={(e) => setCalcAttended(Math.max(0, parseInt(e.target.value) || 0))}
                          />
                        </div>
                      </div>

                      <div className="calc-output-box">
                        <div className="calc-pct-display">
                          <span className="label">Current Attendance</span>
                          <span className={`val ${customCalcResult.status}`}>
                            {customCalcResult.pctDisplay}
                          </span>
                        </div>

                        <div className="calc-decision-card">
                          {customCalcResult.status === 'safe' ? (
                            <div className="decision-safe">
                              <CheckCircle size={22} className="icon-safe" />
                              <div className="text">
                                <h4>You are comfortably above 75%!</h4>
                                <p>
                                  You can safely bunk <strong>{customCalcResult.safeCount} classes</strong> without dropping below the 75% criteria.
                                </p>
                              </div>
                            </div>
                          ) : (
                            <div className="decision-danger">
                              <AlertTriangle size={22} className="icon-danger" />
                              <div className="text">
                                <h4>Attendance Shortage Alert</h4>
                                <p>
                                  You need to attend <strong>{customCalcResult.needCount} consecutive classes</strong> without bunking to restore your attendance to 75%.
                                </p>
                              </div>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
