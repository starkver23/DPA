import { useEffect, useState } from 'react';
import { Sun, Moon } from 'lucide-react';

const getLocalStorageTheme = (): string => {
  try {
    if (typeof window !== 'undefined' && window.localStorage && typeof window.localStorage.getItem === 'function') {
      return window.localStorage.getItem('theme') || 'dark';
    }
  } catch (e) {}
  return 'dark';
};

const setLocalStorageTheme = (theme: string): void => {
  try {
    if (typeof window !== 'undefined' && window.localStorage && typeof window.localStorage.setItem === 'function') {
      window.localStorage.setItem('theme', theme);
    }
  } catch (e) {}
};

export default function ThemeToggle() {
  const [theme, setTheme] = useState(getLocalStorageTheme);

  useEffect(() => {
    try {
      document.documentElement.setAttribute('data-theme', theme);
    } catch (e) {}
  }, [theme]);

  const toggleTheme = () => {
    const newTheme = theme === 'dark' ? 'light' : 'dark';
    setTheme(newTheme);
    setLocalStorageTheme(newTheme);
  };

  return (
    <button 
      onClick={toggleTheme} 
      className="btn-theme-toggle"
      aria-label="Toggle dark/light theme"
    >
      {theme === 'dark' ? <Sun size={15} /> : <Moon size={15} />}
    </button>
  );
}
