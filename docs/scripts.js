/* ─── Language switching ─── */

function resolvePath(obj, path) {
  return path.split('.').reduce((acc, key) => acc && acc[key], obj);
}

function switchLang(lang) {
  const texts = i18n[lang];
  if (!texts) return;

  document.documentElement.lang = lang;
  document.documentElement.setAttribute('data-lang', lang);

  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.getAttribute('data-i18n');
    let val = resolvePath(texts, key);
    if (val === undefined) val = resolvePath(i18n['pt-BR'], key);
    if (val === undefined) return;

    if (el.tagName === 'TITLE' || el.tagName === 'META') {
      el.textContent = val.replace(/<br\s*\/?>/g, '');
      return;
    }

    el.innerHTML = val;
  });

  document.querySelectorAll('.fdroid-link').forEach(el => {
    const locale = lang === 'pt-BR' ? 'pt_BR' : 'en';
    el.href = 'https://f-droid.org/' + locale + '/packages/com.biketrackd.app/';
  });

  document.querySelectorAll('.lang-option').forEach(el => {
    el.classList.toggle('active', el.getAttribute('data-lang') === lang);
  });

  try { localStorage.setItem('biketrackd-lang', lang); } catch (e) {}
}

function detectLang() {
  try {
    const saved = localStorage.getItem('biketrackd-lang');
    if (saved && i18n[saved]) return saved;
  } catch (e) {}
  const nav = (navigator.language || '').toLowerCase();
  return nav.startsWith('pt') ? 'pt-BR' : 'en-US';
}

/* ─── DOM Ready ─── */

document.addEventListener('DOMContentLoaded', () => {

  /* ─── Init language ─── */
  const initialLang = detectLang();
  switchLang(initialLang);

  /* ─── Lang toggle ─── */
  document.querySelectorAll('.lang-option').forEach(el => {
    el.addEventListener('click', () => {
      const lang = el.getAttribute('data-lang');
      if (lang && i18n[lang]) switchLang(lang);
    });
  });

  /* ─── Navbar scroll effect ─── */
  const navbar = document.getElementById('navbar');

  const onScroll = () => {
    const scrollY = window.scrollY;
    navbar.classList.toggle('scrolled', scrollY > 60);
  };

  window.addEventListener('scroll', onScroll, { passive: true });

  /* ─── Mobile nav toggle ─── */
  const toggle = document.getElementById('navToggle');
  const navLinks = document.querySelector('.nav-links');

  toggle.addEventListener('click', () => {
    navLinks.classList.toggle('open');
  });

  document.querySelectorAll('.nav-links a').forEach(link => {
    link.addEventListener('click', () => {
      navLinks.classList.remove('open');
    });
  });

  /* ─── Fade-in on scroll ─── */
  const fadeElements = document.querySelectorAll('.fade-in');

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible');
        observer.unobserve(entry.target);
      }
    });
  }, {
    threshold: 0.10,
    rootMargin: '0px 0px -40px 0px'
  });

  fadeElements.forEach(el => observer.observe(el));

});
