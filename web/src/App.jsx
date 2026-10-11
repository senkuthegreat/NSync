import React, { useState, useEffect } from 'react';
import {
  Download,
  ShieldCheck,
  Percent,
  LayoutGrid,
  WifiOff,
  Clock,
  Award,
  CalendarDays,
  Ticket,
  Globe,
  Mail
} from 'lucide-react';

// Direct package download hosted locally on the site CDN (Zero GitHub links exposed for APK)
const LATEST_DOWNLOAD_URL = './downloads/NSync.apk';

// Dedicated high-resolution artwork: Pink Sunset Clouds (matching reference)
const HERO_ARTWORK = './assets/artworks/sunset-clouds.jpg';

// Developer Profile & Social Data
const DEVELOPER_INFO = {
  name: 'Rohit Dahiya',
  handle: '@senkuthegreat',
  avatar: 'https://avatars.githubusercontent.com/u/126081063?v=4',
  portfolio: 'https://senkuthegreat.me',
  github: 'https://github.com/senkuthegreat',
  linkedin: 'https://www.linkedin.com/in/rohit-dahiya-14b180240/',
  instagram: 'https://instagram.com/senkuthegreat',
  email: 'mailto:rohitdahiya245@gmail.com'
};

function GithubIcon({ size = 16, className = '' }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" className={className}>
      <path fillRule="evenodd" clipRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"/>
    </svg>
  );
}

function LinkedinIcon({ size = 16, className = '' }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" className={className}>
      <path d="M19 3a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h14m-.5 15.5v-5.3a3.26 3.26 0 0 0-3.26-3.26c-.85 0-1.84.52-2.28 1.3v-1.11h-2.79v8.37h2.79v-4.93c0-.77.62-1.4 1.39-1.4a1.4 1.4 0 0 1 1.4 1.4v4.93h2.75M6.46 10.9h2.79v8.37H6.46v-8.37M7.86 6.54a1.63 1.63 0 1 0 0 3.26 1.63 1.63 0 0 0 0-3.26z"/>
    </svg>
  );
}

function InstagramIcon({ size = 16, className = '' }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" className={className}>
      <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z"/>
    </svg>
  );
}

export default function App() {
  const [scrolled, setScrolled] = useState(false);

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

          {/* Top Clean Header: Logo on Left, ONLY Contact Pill on Right */}
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
              {/* Creator Contact Pill in Header */}
              <div className="nav-social-group">
                <a
                  href={DEVELOPER_INFO.portfolio}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="nav-social-icon-btn"
                  title="Portfolio (senkuthegreat.me)"
                  aria-label="Portfolio"
                >
                  <Globe size={15} />
                </a>
                <a
                  href={DEVELOPER_INFO.github}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="nav-social-icon-btn"
                  title="GitHub (@senkuthegreat)"
                  aria-label="GitHub"
                >
                  <GithubIcon size={15} />
                </a>
                <a
                  href={DEVELOPER_INFO.linkedin}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="nav-social-icon-btn"
                  title="LinkedIn (Rohit Dahiya)"
                  aria-label="LinkedIn"
                >
                  <LinkedinIcon size={15} />
                </a>
                <a
                  href={DEVELOPER_INFO.instagram}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="nav-social-icon-btn"
                  title="Instagram (@senkuthegreat)"
                  aria-label="Instagram"
                >
                  <InstagramIcon size={15} />
                </a>
                <a
                  href={DEVELOPER_INFO.email}
                  className="nav-social-icon-btn"
                  title="Email (rohitdahiya245@gmail.com)"
                  aria-label="Email"
                >
                  <Mail size={15} />
                </a>
              </div>
            </div>
          </header>

          {/* Hero Headlines & Single Prominent CTA */}
          <div className="hero-hero-content">
            <h1 className="hero-main-title">Academic Life, Seamlessly Synced.</h1>
            <p className="hero-main-subtitle">
              Attendance tracking with 75% criteria calculator, daily timetables,
              semester results, academic calendar, and admit card seating — built exclusively for NSUT students.
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
                cached locally on your device so you never have to re-enter
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
                <Clock size={24} />
              </div>
              <h3>Daily Timetables & Google Cal</h3>
              <p>
                View period times, rooms, and professors. Export your entire semester
                schedule directly into Google Calendar with 1-tap reminders.
              </p>
            </div>

            <div className="feature-box">
              <div className="icon-badge">
                <Award size={24} />
              </div>
              <h3>Results & CGPA Tracker</h3>
              <p>
                Semester-wise SGPA breakdowns, cumulative CGPA tracking, and
                official grade sheets delivered without navigating the slow IMS site.
              </p>
            </div>

            <div className="feature-box">
              <div className="icon-badge">
                <CalendarDays size={24} />
              </div>
              <h3>Official Academic Calendar</h3>
              <p>
                Stay ahead of mid-term examinations, end-sem evaluations,
                vacations, and gazetted university holidays with an integrated calendar.
              </p>
            </div>

            <div className="feature-box">
              <div className="icon-badge">
                <Ticket size={24} />
              </div>
              <h3>Admit Card & Exam Seating</h3>
              <p>
                Instant access to verified roll number-wise hall allocations,
                desk numbers, and circular examination notices.
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
                Sign in once with your IMS credentials. Your attendance, results,
                and timetable sync automatically.
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
              Technology. Fast, offline-first, and private.
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
            </div>
          </div>
        </section>

        {/* Clean, Professional Footer (Zero Redundant Contact Cards) */}
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
              Student companion for NSUT. Not officially affiliated with university administration.
            </p>
          </div>

          <div className="footer-right">
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
