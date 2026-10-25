import React, { useState, useEffect, useRef } from 'react';
import {
  getAllScripts,
  saveScript,
  deleteScript,
  calculateWordCount,
  calculateDurationSec,
  formatDuration
} from './db';
import { Script, PrompterSettings, CameraFilter } from './types';
import {
  Play,
  Pause,
  RotateCcw,
  Plus,
  Search,
  Upload,
  Download,
  Share2,
  Trash2,
  Edit3,
  Star,
  Camera,
  Layers,
  Settings,
  Sliders,
  Type,
  FlipHorizontal,
  Mic,
  MicOff,
  Video,
  X,
  FileText,
  HelpCircle,
  Shield,
  ExternalLink,
  ChevronLeft
} from 'lucide-react';

const DEFAULT_SETTINGS: PrompterSettings = {
  scrollSpeed: 130,
  fontSizePx: 32,
  textColor: '#ffffff',
  backgroundColor: '#080c14',
  backgroundOpacity: 0.85,
  textAlign: 'center',
  mirrorMode: false,
  countdownSeconds: 3,
  voiceFollowEnabled: false,
  speechLanguage: 'hi-IN',
  cameraFacing: 'user',
  cameraMirror: true,
  cameraFilter: 'none'
};

export default function App() {
  const [currentTab, setCurrentTab] = useState<'home' | 'editor' | 'prompter' | 'camera' | 'floating' | 'settings'>('home');
  const [scripts, setScripts] = useState<Script[]>([]);
  const [activeScript, setActiveScript] = useState<Script | null>(null);
  const [settings, setSettings] = useState<PrompterSettings>(() => {
    const saved = localStorage.getItem('prompter_settings');
    return saved ? JSON.parse(saved) : DEFAULT_SETTINGS;
  });

  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState<'all' | 'fav' | 'hi' | 'en'>('all');
  const [sortBy, setSortBy] = useState<'date' | 'title' | 'duration'>('date');

  // Load scripts from IndexedDB on startup
  useEffect(() => {
    loadScripts();
  }, []);

  useEffect(() => {
    localStorage.setItem('prompter_settings', JSON.stringify(settings));
  }, [settings]);

  const loadScripts = async () => {
    try {
      const list = await getAllScripts();
      setScripts(list);
      if (!activeScript && list.length > 0) {
        setActiveScript(list[0]);
      }
    } catch (e) {
      console.error('Failed to load scripts', e);
    }
  };

  const handleCreateScript = () => {
    const newScript: Script = {
      id: 0,
      title: 'नया वीडियो स्क्रिप्ट (Untitled)',
      content: '',
      language: 'hi',
      wordCount: 0,
      estimatedDurationSec: 0,
      updatedAt: Date.now(),
      createdAt: Date.now(),
      isFavorite: false
    };
    setActiveScript(newScript);
    setCurrentTab('editor');
  };

  const handleSaveActiveScript = async (updated: Partial<Script>) => {
    try {
      const id = await saveScript({ ...(activeScript || {}), ...updated });
      await loadScripts();
      const all = await getAllScripts();
      const saved = all.find((s) => s.id === id) || null;
      setActiveScript(saved);
      return saved;
    } catch (e) {
      console.error('Failed to save script', e);
    }
  };

  const handleDeleteScript = async (id: number) => {
    if (confirm('क्या आप इस स्क्रिप्ट को हटाना चाहते हैं? (Delete this script?)')) {
      await deleteScript(id);
      await loadScripts();
      if (activeScript?.id === id) {
        setActiveScript(null);
      }
    }
  };

  const filteredScripts = scripts.filter((s) => {
    const matchSearch =
      s.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.content.toLowerCase().includes(searchQuery.toLowerCase());
    if (!matchSearch) return false;
    if (filterType === 'fav') return s.isFavorite;
    if (filterType === 'hi') return s.language === 'hi' || s.language === 'mixed';
    if (filterType === 'en') return s.language === 'en';
    return true;
  }).sort((a, b) => {
    if (sortBy === 'title') return a.title.localeCompare(b.title);
    if (sortBy === 'duration') return b.estimatedDurationSec - a.estimatedDurationSec;
    return b.updatedAt - a.updatedAt;
  });

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: '#080c14', color: '#f8fafc' }}>
      {/* Top Navbar */}
      <header style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '12px 18px',
        backgroundColor: '#0f172a',
        borderBottom: '1px solid #1e293b'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }} onClick={() => setCurrentTab('home')}>
          <div style={{
            width: '32px',
            height: '32px',
            borderRadius: '8px',
            backgroundColor: 'rgba(0, 229, 255, 0.15)',
            border: '1px solid #00e5ff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#00e5ff',
            fontWeight: 'bold',
            fontSize: '16px'
          }}>
            T
          </div>
          <span style={{ fontSize: '18px', fontWeight: 'bold', color: '#f8fafc', letterSpacing: '0.5px' }}>
            Teleprompter <span style={{ color: '#00e5ff' }}>PRO</span>
          </span>
        </div>

        {/* Global Navigation Tabs */}
        <nav style={{ display: 'flex', gap: '8px' }}>
          <button
            onClick={() => setCurrentTab('home')}
            style={navBtnStyle(currentTab === 'home')}
          >
            <FileText size={16} /> <span className="tab-label">Scripts</span>
          </button>
          <button
            onClick={() => {
              if (activeScript) setCurrentTab('camera');
              else if (scripts.length > 0) { setActiveScript(scripts[0]); setCurrentTab('camera'); }
              else handleCreateScript();
            }}
            style={navBtnStyle(currentTab === 'camera')}
          >
            <Camera size={16} /> <span className="tab-label">Studio</span>
          </button>
          <button
            onClick={() => {
              if (activeScript) setCurrentTab('floating');
              else if (scripts.length > 0) { setActiveScript(scripts[0]); setCurrentTab('floating'); }
              else handleCreateScript();
            }}
            style={navBtnStyle(currentTab === 'floating')}
          >
            <Layers size={16} /> <span className="tab-label">Floating</span>
          </button>
          <button
            onClick={() => setCurrentTab('settings')}
            style={navBtnStyle(currentTab === 'settings')}
          >
            <Settings size={16} /> <span className="tab-label">More</span>
          </button>
        </nav>
      </header>

      {/* Main Content Area */}
      <main style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        {currentTab === 'home' && (
          <HomeScreen
            scripts={filteredScripts}
            searchQuery={searchQuery}
            setSearchQuery={setSearchQuery}
            filterType={filterType}
            setFilterType={setFilterType}
            sortBy={sortBy}
            setSortBy={setSortBy}
            onCreate={handleCreateScript}
            onOpenScript={(s) => { setActiveScript(s); setCurrentTab('editor'); }}
            onPrompter={(s) => { setActiveScript(s); setCurrentTab('prompter'); }}
            onCamera={(s) => { setActiveScript(s); setCurrentTab('camera'); }}
            onFloating={(s) => { setActiveScript(s); setCurrentTab('floating'); }}
            onToggleFavorite={async (s) => {
              await saveScript({ ...s, isFavorite: !s.isFavorite });
              loadScripts();
            }}
            onDeleteScript={handleDeleteScript}
            onImportTxt={async (file) => {
              const text = await file.text();
              const title = file.name.replace(/\.[^/.]+$/, '');
              await saveScript({
                title,
                content: text,
                language: /[\u0900-\u097F]/.test(text) ? 'hi' : 'en'
              });
              loadScripts();
            }}
          />
        )}

        {currentTab === 'editor' && activeScript && (
          <ScriptEditor
            script={activeScript}
            onBack={() => setCurrentTab('home')}
            onSave={handleSaveActiveScript}
            onStartPrompter={() => setCurrentTab('prompter')}
            onStartCamera={() => setCurrentTab('camera')}
            onStartFloating={() => setCurrentTab('floating')}
          />
        )}

        {currentTab === 'prompter' && activeScript && (
          <TeleprompterPlayer
            script={activeScript}
            settings={settings}
            onUpdateSettings={(s) => setSettings({ ...settings, ...s })}
            onBack={() => setCurrentTab('home')}
          />
        )}

        {currentTab === 'camera' && activeScript && (
          <CameraStudio
            script={activeScript}
            settings={settings}
            onUpdateSettings={(s) => setSettings({ ...settings, ...s })}
            onBack={() => setCurrentTab('home')}
          />
        )}

        {currentTab === 'floating' && activeScript && (
          <FloatingTeleprompterScreen
            script={activeScript}
            settings={settings}
            onUpdateSettings={(s) => setSettings({ ...settings, ...s })}
            onBack={() => setCurrentTab('home')}
          />
        )}

        {currentTab === 'settings' && (
          <SettingsScreen
            settings={settings}
            onUpdateSettings={(s) => setSettings({ ...settings, ...s })}
            scripts={scripts}
          />
        )}
      </main>

      <style>{`
        @media (max-width: 600px) {
          .tab-label { display: none; }
        }
      `}</style>
    </div>
  );
}

function navBtnStyle(active: boolean): React.CSSProperties {
  return {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
    padding: '8px 14px',
    borderRadius: '10px',
    border: active ? '1px solid #00e5ff' : '1px solid #334155',
    backgroundColor: active ? 'rgba(0, 229, 255, 0.15)' : '#1e293b',
    color: active ? '#00e5ff' : '#94a3b8',
    cursor: 'pointer',
    fontWeight: active ? '600' : '500',
    fontSize: '13px',
    transition: 'all 0.15s ease'
  };
}

/* ====================================================================
   1. HOME SCREEN COMPONENT
   ==================================================================== */
function HomeScreen({
  scripts,
  searchQuery,
  setSearchQuery,
  filterType,
  setFilterType,
  sortBy,
  setSortBy,
  onCreate,
  onOpenScript,
  onPrompter,
  onCamera,
  onFloating,
  onToggleFavorite,
  onDeleteScript,
  onImportTxt
}: any) {
  const fileInputRef = useRef<HTMLInputElement>(null);

  return (
    <div style={{ maxWidth: '960px', width: '100%', margin: '0 auto', padding: '16px' }}>
      {/* Search and Filters Bar */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '20px' }}>
        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          <div style={{
            position: 'relative',
            flex: 1,
            display: 'flex',
            alignItems: 'center',
            backgroundColor: '#1e293b',
            borderRadius: '12px',
            border: '1px solid #334155',
            padding: '0 12px'
          }}>
            <Search size={18} color="#94a3b8" />
            <input
              type="text"
              placeholder="Search scripts (हिंदी / English)..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{
                width: '100%',
                padding: '12px',
                backgroundColor: 'transparent',
                border: 'none',
                outline: 'none',
                color: '#f8fafc',
                fontSize: '14px'
              }}
            />
          </div>

          <button
            onClick={() => fileInputRef.current?.click()}
            title="Import TXT File"
            style={{
              padding: '12px 16px',
              backgroundColor: '#1e293b',
              color: '#00e5ff',
              border: '1px solid #334155',
              borderRadius: '12px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              fontWeight: '600',
              fontSize: '13px'
            }}
          >
            <Upload size={16} /> Import TXT
          </button>
          <input
            type="file"
            ref={fileInputRef}
            style={{ display: 'none' }}
            accept=".txt"
            onChange={(e) => {
              if (e.target.files && e.target.files[0]) {
                onImportTxt(e.target.files[0]);
              }
            }}
          />
        </div>

        {/* Filter Chips & Sort Select */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '8px' }}>
          <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
            <button
              onClick={() => setFilterType('all')}
              style={filterChipStyle(filterType === 'all')}
            >
              All ({scripts.length})
            </button>
            <button
              onClick={() => setFilterType('fav')}
              style={filterChipStyle(filterType === 'fav')}
            >
              ★ Favorites
            </button>
            <button
              onClick={() => setFilterType('hi')}
              style={filterChipStyle(filterType === 'hi')}
            >
              हिन्दी Scripts
            </button>
            <button
              onClick={() => setFilterType('en')}
              style={filterChipStyle(filterType === 'en')}
            >
              English
            </button>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', color: '#94a3b8' }}>
            <span>Sort by:</span>
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as any)}
              style={{
                backgroundColor: '#1e293b',
                color: '#f8fafc',
                border: '1px solid #334155',
                padding: '6px 10px',
                borderRadius: '8px',
                outline: 'none',
                cursor: 'pointer'
              }}
            >
              <option value="date">Last Modified</option>
              <option value="title">Title (A-Z)</option>
              <option value="duration">Reading Time</option>
            </select>
          </div>
        </div>
      </div>

      {/* Script List */}
      {scripts.length === 0 ? (
        <div style={{
          textAlign: 'center',
          padding: '60px 20px',
          backgroundColor: '#0f172a',
          borderRadius: '16px',
          border: '1px dashed #334155'
        }}>
          <FileText size={48} color="#00e5ff" style={{ margin: '0 auto 16px' }} />
          <h3 style={{ fontSize: '18px', fontWeight: 'bold', marginBottom: '8px' }}>No Scripts Found</h3>
          <p style={{ color: '#94a3b8', fontSize: '14px', maxWidth: '400px', margin: '0 auto 20px' }}>
            Create your first teleprompter script in Hindi or English, or import a TXT file to start recording.
          </p>
          <button
            onClick={onCreate}
            style={{
              padding: '12px 24px',
              backgroundColor: '#00e5ff',
              color: '#080c14',
              borderRadius: '12px',
              border: 'none',
              fontWeight: 'bold',
              cursor: 'pointer',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '8px'
            }}
          >
            <Plus size={18} /> Create New Script
          </button>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '16px' }}>
          {scripts.map((script: Script) => (
            <div
              key={script.id}
              style={{
                backgroundColor: '#0f172a',
                borderRadius: '16px',
                border: '1px solid #1e293b',
                padding: '18px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                transition: 'border 0.2s ease',
                position: 'relative'
              }}
            >
              {/* Header */}
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                  <h3
                    onClick={() => onOpenScript(script)}
                    style={{
                      fontSize: '16px',
                      fontWeight: 'bold',
                      color: '#f8fafc',
                      cursor: 'pointer',
                      marginRight: '8px',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap'
                    }}
                  >
                    {script.title}
                  </h3>
                  <button
                    onClick={() => onToggleFavorite(script)}
                    style={{ background: 'none', border: 'none', cursor: 'pointer', color: script.isFavorite ? '#f59e0b' : '#64748b' }}
                  >
                    <Star size={18} fill={script.isFavorite ? '#f59e0b' : 'none'} />
                  </button>
                </div>

                <p
                  onClick={() => onOpenScript(script)}
                  style={{
                    fontSize: '14px',
                    color: '#94a3b8',
                    lineHeight: '1.5',
                    maxHeight: '64px',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    display: '-webkit-box',
                    WebkitLineClamp: 3,
                    WebkitBoxOrient: 'vertical',
                    cursor: 'pointer',
                    marginBottom: '14px'
                  }}
                >
                  {script.content || '(Empty script content)'}
                </p>

                {/* Badges */}
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap', marginBottom: '16px' }}>
                  <span style={badgeStyle('#334155', '#cbd5e1')}>{script.wordCount} words</span>
                  <span style={badgeStyle('#334155', '#00e5ff')}>⏱ {formatDuration(script.estimatedDurationSec)}</span>
                  {script.language === 'hi' && <span style={badgeStyle('rgba(0, 229, 255, 0.15)', '#00e5ff')}>हिंदी</span>}
                  {script.language === 'mixed' && <span style={badgeStyle('rgba(245, 158, 11, 0.15)', '#f59e0b')}>हिंदी + EN</span>}
                </div>
              </div>

              {/* Action Buttons Row */}
              <div style={{ display: 'flex', gap: '8px', borderTop: '1px solid #1e293b', paddingTop: '12px' }}>
                <button
                  onClick={() => onPrompter(script)}
                  style={cardActionBtnStyle('#00e5ff', '#080c14')}
                  title="Fullscreen Prompter"
                >
                  <Play size={14} fill="#080c14" /> Prompter
                </button>
                <button
                  onClick={() => onCamera(script)}
                  style={cardActionBtnStyle('#0284c7', '#ffffff')}
                  title="Camera Studio"
                >
                  <Camera size={14} /> Studio
                </button>
                <button
                  onClick={() => onFloating(script)}
                  style={cardActionBtnStyle('#334155', '#00e5ff')}
                  title="Floating Overlay"
                >
                  <Layers size={14} /> Floating
                </button>
                <button
                  onClick={() => onDeleteScript(script.id)}
                  style={{
                    padding: '8px 10px',
                    backgroundColor: 'rgba(239, 68, 68, 0.1)',
                    border: '1px solid rgba(239, 68, 68, 0.2)',
                    borderRadius: '8px',
                    color: '#ef4444',
                    cursor: 'pointer'
                  }}
                  title="Delete"
                >
                  <Trash2 size={14} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Floating Action Button for Mobile */}
      <button
        onClick={onCreate}
        style={{
          position: 'fixed',
          bottom: '24px',
          right: '24px',
          width: '56px',
          height: '56px',
          borderRadius: '28px',
          backgroundColor: '#00e5ff',
          color: '#080c14',
          border: 'none',
          boxShadow: '0 8px 24px rgba(0, 229, 255, 0.4)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          cursor: 'pointer',
          zIndex: 50
        }}
        title="Create Script"
      >
        <Plus size={28} />
      </button>
    </div>
  );
}

function filterChipStyle(active: boolean): React.CSSProperties {
  return {
    padding: '6px 12px',
    borderRadius: '20px',
    border: active ? '1px solid #00e5ff' : '1px solid #334155',
    backgroundColor: active ? 'rgba(0, 229, 255, 0.15)' : '#1e293b',
    color: active ? '#00e5ff' : '#94a3b8',
    fontSize: '12px',
    fontWeight: '500',
    cursor: 'pointer'
  };
}

function badgeStyle(bg: string, color: string): React.CSSProperties {
  return {
    padding: '4px 8px',
    borderRadius: '6px',
    backgroundColor: bg,
    color: color,
    fontSize: '11px',
    fontWeight: '600'
  };
}

function cardActionBtnStyle(bg: string, color: string): React.CSSProperties {
  return {
    flex: 1,
    padding: '8px',
    borderRadius: '8px',
    backgroundColor: bg,
    color: color,
    border: 'none',
    fontWeight: '600',
    fontSize: '12px',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '4px'
  };
}

/* ====================================================================
   2. SCRIPT EDITOR COMPONENT
   ==================================================================== */
function ScriptEditor({ script, onBack, onSave, onStartPrompter, onStartCamera, onStartFloating }: any) {
  const [title, setTitle] = useState(script.title);
  const [content, setContent] = useState(script.content);
  const [language, setLanguage] = useState<'hi' | 'en' | 'mixed'>(script.language || 'hi');
  const [savedStatus, setSavedStatus] = useState<'saved' | 'saving'>('saved');

  const wordCount = calculateWordCount(content);
  const durationSec = calculateDurationSec(wordCount);

  // Auto-save debounce
  useEffect(() => {
    setSavedStatus('saving');
    const timer = setTimeout(() => {
      onSave({ title, content, language });
      setSavedStatus('saved');
    }, 1200);
    return () => clearTimeout(timer);
  }, [title, content, language]);

  const handleExportTxt = () => {
    const blob = new Blob([`${title}\n\n${content}`], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${title.replace(/[^a-zA-Z0-9_-]/g, '_')}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', maxWidth: '960px', width: '100%', margin: '0 auto', padding: '16px' }}>
      {/* Top Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
        <button
          onClick={onBack}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '4px',
            backgroundColor: '#1e293b',
            color: '#f8fafc',
            border: '1px solid #334155',
            padding: '8px 14px',
            borderRadius: '10px',
            cursor: 'pointer',
            fontSize: '13px'
          }}
        >
          <ChevronLeft size={16} /> Back
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '12px', color: savedStatus === 'saved' ? '#10b981' : '#00e5ff' }}>
            ● {savedStatus === 'saved' ? 'Auto-Saved' : 'Saving...'}
          </span>
          <button
            onClick={handleExportTxt}
            title="Export as TXT file"
            style={{
              padding: '8px 12px',
              backgroundColor: '#1e293b',
              color: '#00e5ff',
              border: '1px solid #334155',
              borderRadius: '8px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
              fontSize: '12px'
            }}
          >
            <Download size={14} /> Export TXT
          </button>
        </div>
      </div>

      {/* Title Input */}
      <input
        type="text"
        placeholder="Script Title (e.g. YouTube Video / हिंदी स्पीच)"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        style={{
          fontSize: '20px',
          fontWeight: 'bold',
          padding: '14px',
          backgroundColor: '#0f172a',
          color: '#f8fafc',
          border: '1px solid #334155',
          borderRadius: '12px',
          marginBottom: '12px',
          outline: 'none'
        }}
      />

      {/* Metadata Bar */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px', flexWrap: 'wrap', gap: '8px' }}>
        <div style={{ display: 'flex', gap: '10px', alignItems: 'center', fontSize: '13px', color: '#94a3b8' }}>
          <span style={{ color: '#00e5ff', fontWeight: 'bold' }}>{wordCount} words</span>
          <span>•</span>
          <span style={{ color: '#f59e0b', fontWeight: 'bold' }}>⏱ {formatDuration(durationSec)} min reading</span>
        </div>

        <div style={{ display: 'flex', gap: '6px' }}>
          <button
            onClick={() => setLanguage('hi')}
            style={filterChipStyle(language === 'hi')}
          >
            हिंदी
          </button>
          <button
            onClick={() => setLanguage('en')}
            style={filterChipStyle(language === 'en')}
          >
            English
          </button>
          <button
            onClick={() => setLanguage('mixed')}
            style={filterChipStyle(language === 'mixed')}
          >
            Mixed
          </button>
        </div>
      </div>

      {/* Main Textarea */}
      <textarea
        placeholder="यहाँ अपना वीडियो स्क्रिप्ट लिखें या पेस्ट करें... Type or paste your video script here in Hindi or English."
        value={content}
        onChange={(e) => setContent(e.target.value)}
        style={{
          flex: 1,
          minHeight: '360px',
          padding: '16px',
          backgroundColor: '#0f172a',
          color: '#f8fafc',
          border: '1px solid #334155',
          borderRadius: '14px',
          fontSize: '17px',
          lineHeight: '1.7',
          resize: 'none',
          outline: 'none',
          fontFamily: "'Poppins', 'Noto Sans Devanagari', sans-serif"
        }}
      />

      {/* Quick Launch Action Bar */}
      <div style={{ display: 'flex', gap: '12px', marginTop: '16px' }}>
        <button
          onClick={onStartPrompter}
          style={{
            flex: 1,
            padding: '14px',
            backgroundColor: '#00e5ff',
            color: '#080c14',
            borderRadius: '12px',
            border: 'none',
            fontWeight: 'bold',
            fontSize: '15px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '8px'
          }}
        >
          <Play size={18} fill="#080c14" /> Start Prompter
        </button>

        <button
          onClick={onStartCamera}
          style={{
            flex: 1,
            padding: '14px',
            backgroundColor: '#0284c7',
            color: '#ffffff',
            borderRadius: '12px',
            border: 'none',
            fontWeight: 'bold',
            fontSize: '15px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '8px'
          }}
        >
          <Camera size={18} /> Camera Studio
        </button>

        <button
          onClick={onStartFloating}
          style={{
            flex: 1,
            padding: '14px',
            backgroundColor: '#1e293b',
            color: '#00e5ff',
            borderRadius: '12px',
            border: '1px solid #00e5ff',
            fontWeight: 'bold',
            fontSize: '15px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '8px'
          }}
        >
          <Layers size={18} /> Floating Window
        </button>
      </div>
    </div>
  );
}

/* ====================================================================
   3. TELEPROMPTER PLAYER COMPONENT (FULLSCREEN)
   ==================================================================== */
function TeleprompterPlayer({ script, settings, onUpdateSettings, onBack }: any) {
  const [isPlaying, setIsPlaying] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const [showControls, setShowControls] = useState(true);
  const [voiceFollowActive, setVoiceFollowActive] = useState(settings.voiceFollowEnabled);
  const [recognizedText, setRecognizedText] = useState('');
  const [activeSettingsTab, setActiveSettingsTab] = useState<'speed' | 'font' | null>(null);

  const containerRef = useRef<HTMLDivElement>(null);
  const animFrameRef = useRef<number | null>(null);

  // Auto-scroll loop via requestAnimationFrame
  useEffect(() => {
    let lastTime = performance.now();

    const loop = (currentTime: number) => {
      if (isPlaying && containerRef.current) {
        const delta = (currentTime - lastTime) / 1000;
        // 130 WPM corresponds to approx 65 pixels per second
        const pixelsPerSec = (settings.scrollSpeed / 130) * 60 * (settings.fontSizePx / 32);
        containerRef.current.scrollTop += pixelsPerSec * delta;
      }
      lastTime = currentTime;
      if (isPlaying) {
        animFrameRef.current = requestAnimationFrame(loop);
      }
    };

    if (isPlaying) {
      animFrameRef.current = requestAnimationFrame(loop);
    } else if (animFrameRef.current) {
      cancelAnimationFrame(animFrameRef.current);
    }

    return () => {
      if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    };
  }, [isPlaying, settings.scrollSpeed, settings.fontSizePx]);

  // Voice Follow via Web Speech API
  useEffect(() => {
    let recognition: any = null;
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (voiceFollowActive && SpeechRecognition) {
      try {
        recognition = new SpeechRecognition();
        recognition.continuous = true;
        recognition.interimResults = true;
        recognition.lang = settings.speechLanguage || 'hi-IN';

        recognition.onresult = (event: any) => {
          let interim = '';
          for (let i = event.resultIndex; i < event.results.length; ++i) {
            interim += event.results[i][0].transcript;
          }
          setRecognizedText(interim);

          // Fuzzy search in script to align scroll
          const words = interim.trim().toLowerCase().split(/\s+/);
          const lastWord = words[words.length - 1];
          if (lastWord && containerRef.current && script.content) {
            const scriptWords = script.content.toLowerCase().split(/\s+/);
            const idx = scriptWords.findIndex((w: string) => w.includes(lastWord));
            if (idx >= 0) {
              const fraction = idx / scriptWords.length;
              containerRef.current.scrollTop = fraction * (containerRef.current.scrollHeight - containerRef.current.clientHeight);
            }
          }
        };

        recognition.onerror = () => {};
        recognition.onend = () => {
          if (voiceFollowActive) {
            try { recognition.start(); } catch (e) {}
          }
        };

        recognition.start();
      } catch (e) {
        console.error('Speech recognition error', e);
      }
    }

    return () => {
      if (recognition) {
        try { recognition.stop(); } catch (e) {}
      }
    };
  }, [voiceFollowActive, settings.speechLanguage, script.content]);

  const togglePlay = () => {
    if (isPlaying) {
      setIsPlaying(false);
    } else {
      if (settings.countdownSeconds > 0) {
        setCountdown(settings.countdownSeconds);
        let c = settings.countdownSeconds;
        const timer = setInterval(() => {
          c -= 1;
          if (c <= 0) {
            clearInterval(timer);
            setCountdown(0);
            setIsPlaying(true);
          } else {
            setCountdown(c);
          }
        }, 1000);
      } else {
        setIsPlaying(true);
      }
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 100,
        backgroundColor: settings.backgroundColor,
        color: settings.textColor,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden'
      }}
      onClick={() => setShowControls(!showControls)}
    >
      {/* Mirror Mode Container */}
      <div
        ref={containerRef}
        style={{
          flex: 1,
          overflowY: 'auto',
          padding: '0 32px',
          transform: settings.mirrorMode ? 'scaleX(-1)' : 'none',
          position: 'relative'
        }}
      >
        {/* Reading Focus Guide Band in Upper Third */}
        <div
          style={{
            position: 'fixed',
            top: '35%',
            left: '20px',
            right: '20px',
            height: '90px',
            borderRadius: '16px',
            backgroundColor: 'rgba(0, 229, 255, 0.12)',
            border: '1.5px solid rgba(0, 229, 255, 0.4)',
            pointerEvents: 'none',
            zIndex: 10
          }}
        />

        {/* Start offset spacer so text starts at reading line */}
        <div style={{ height: '35vh' }} />

        <div
          style={{
            maxWidth: '900px',
            margin: '0 auto',
            fontSize: `${settings.fontSizePx}px`,
            lineHeight: 1.6,
            textAlign: settings.textAlign,
            fontWeight: '500',
            fontFamily: "'Poppins', 'Noto Sans Devanagari', sans-serif"
          }}
        >
          {script.content}
        </div>

        {/* End spacer */}
        <div style={{ height: '70vh' }} />
      </div>

      {/* Countdown overlay */}
      {countdown > 0 && (
        <div style={{
          position: 'absolute',
          inset: 0,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: 'rgba(0,0,0,0.6)',
          zIndex: 150
        }}>
          <div style={{
            width: '120px',
            height: '120px',
            borderRadius: '60px',
            backgroundColor: '#00e5ff',
            color: '#080c14',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '64px',
            fontWeight: '900'
          }}>
            {countdown}
          </div>
        </div>
      )}

      {/* Voice Follow Floating Badge */}
      {voiceFollowActive && (
        <div style={{
          position: 'fixed',
          top: '70px',
          left: '50%',
          transform: 'translateX(-50%)',
          backgroundColor: 'rgba(15, 23, 42, 0.9)',
          border: '1px solid #00e5ff',
          padding: '6px 14px',
          borderRadius: '20px',
          fontSize: '12px',
          color: '#00e5ff',
          zIndex: 110,
          display: 'flex',
          alignItems: 'center',
          gap: '8px'
        }}>
          <span style={{ width: '8px', height: '8px', borderRadius: '4px', backgroundColor: '#10b981' }} />
          Voice Follow: {recognizedText ? recognizedText.slice(-25) : 'Listening for speech...'}
        </div>
      )}

      {/* Collapsible HUD Controls (Click to show/hide) */}
      {showControls && (
        <div
          onClick={(e) => e.stopPropagation()}
          style={{
            position: 'fixed',
            bottom: 0,
            left: 0,
            right: 0,
            backgroundColor: 'rgba(15, 23, 42, 0.95)',
            borderTop: '1px solid #1e293b',
            padding: '14px 20px',
            zIndex: 120,
            display: 'flex',
            flexDirection: 'column',
            gap: '12px'
          }}
        >
          {/* Sub drawer if speed or font active */}
          {activeSettingsTab === 'speed' && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '14px', backgroundColor: '#1e293b', padding: '10px 16px', borderRadius: '12px' }}>
              <span style={{ fontSize: '13px', color: '#94a3b8', minWidth: '70px' }}>Speed:</span>
              <input
                type="range"
                min="50"
                max="320"
                value={settings.scrollSpeed}
                onChange={(e) => onUpdateSettings({ scrollSpeed: Number(e.target.value) })}
                style={{ flex: 1, accentColor: '#00e5ff' }}
              />
              <span style={{ color: '#00e5ff', fontWeight: 'bold', fontSize: '13px', minWidth: '60px' }}>
                {settings.scrollSpeed} WPM
              </span>
            </div>
          )}

          {activeSettingsTab === 'font' && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '14px', backgroundColor: '#1e293b', padding: '10px 16px', borderRadius: '12px' }}>
              <span style={{ fontSize: '13px', color: '#94a3b8', minWidth: '70px' }}>Font Size:</span>
              <input
                type="range"
                min="20"
                max="56"
                value={settings.fontSizePx}
                onChange={(e) => onUpdateSettings({ fontSizePx: Number(e.target.value) })}
                style={{ flex: 1, accentColor: '#00e5ff' }}
              />
              <span style={{ color: '#00e5ff', fontWeight: 'bold', fontSize: '13px', minWidth: '50px' }}>
                {settings.fontSizePx}px
              </span>
            </div>
          )}

          {/* Main Controls Bar */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', maxWidth: '800px', margin: '0 auto', width: '100%' }}>
            <button
              onClick={onBack}
              style={{ padding: '10px', backgroundColor: '#1e293b', color: '#f8fafc', border: 'none', borderRadius: '10px', cursor: 'pointer' }}
            >
              <ChevronLeft size={20} />
            </button>

            <button
              onClick={() => {
                if (containerRef.current) containerRef.current.scrollTop = 0;
                setIsPlaying(false);
              }}
              style={{ padding: '10px', backgroundColor: '#1e293b', color: '#f8fafc', border: 'none', borderRadius: '10px', cursor: 'pointer' }}
              title="Restart"
            >
              <RotateCcw size={18} />
            </button>

            <button
              onClick={() => setActiveSettingsTab(activeSettingsTab === 'speed' ? null : 'speed')}
              style={{
                padding: '10px 14px',
                backgroundColor: activeSettingsTab === 'speed' ? 'rgba(0, 229, 255, 0.2)' : '#1e293b',
                color: activeSettingsTab === 'speed' ? '#00e5ff' : '#f8fafc',
                border: 'none',
                borderRadius: '10px',
                cursor: 'pointer',
                fontSize: '12px',
                fontWeight: 'bold',
                display: 'flex',
                alignItems: 'center',
                gap: '4px'
              }}
            >
              <Sliders size={16} /> {settings.scrollSpeed} wpm
            </button>

            {/* Play/Pause Button */}
            <button
              onClick={togglePlay}
              style={{
                width: '56px',
                height: '56px',
                borderRadius: '28px',
                backgroundColor: '#00e5ff',
                color: '#080c14',
                border: 'none',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 4px 16px rgba(0, 229, 255, 0.4)'
              }}
            >
              {isPlaying ? <Pause size={24} fill="#080c14" /> : <Play size={24} fill="#080c14" />}
            </button>

            <button
              onClick={() => setActiveSettingsTab(activeSettingsTab === 'font' ? null : 'font')}
              style={{
                padding: '10px 14px',
                backgroundColor: activeSettingsTab === 'font' ? 'rgba(0, 229, 255, 0.2)' : '#1e293b',
                color: activeSettingsTab === 'font' ? '#00e5ff' : '#f8fafc',
                border: 'none',
                borderRadius: '10px',
                cursor: 'pointer',
                fontSize: '12px',
                fontWeight: 'bold',
                display: 'flex',
                alignItems: 'center',
                gap: '4px'
              }}
            >
              <Type size={16} /> {settings.fontSizePx}px
            </button>

            <button
              onClick={() => onUpdateSettings({ mirrorMode: !settings.mirrorMode })}
              style={{
                padding: '10px',
                backgroundColor: settings.mirrorMode ? 'rgba(0, 229, 255, 0.2)' : '#1e293b',
                color: settings.mirrorMode ? '#00e5ff' : '#f8fafc',
                border: 'none',
                borderRadius: '10px',
                cursor: 'pointer'
              }}
              title="Mirror Mode"
            >
              <FlipHorizontal size={18} />
            </button>

            <button
              onClick={() => setVoiceFollowActive(!voiceFollowActive)}
              style={{
                padding: '10px',
                backgroundColor: voiceFollowActive ? 'rgba(0, 229, 255, 0.2)' : '#1e293b',
                color: voiceFollowActive ? '#00e5ff' : '#f8fafc',
                border: 'none',
                borderRadius: '10px',
                cursor: 'pointer'
              }}
              title="Voice Follow"
            >
              {voiceFollowActive ? <Mic size={18} /> : <MicOff size={18} />}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

/* ====================================================================
   4. CAMERA STUDIO COMPONENT (LIVE RECORDING + PROMPTER OVERLAY)
   ==================================================================== */
function CameraStudio({ script, settings, onUpdateSettings, onBack }: any) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const prompterBoxRef = useRef<HTMLDivElement>(null);

  const [isCameraActive, setIsCameraActive] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const [recordingSeconds, setRecordingSeconds] = useState(0);
  const [recordedBlobUrl, setRecordedBlobUrl] = useState<string | null>(null);
  const [facingMode, setFacingMode] = useState<'user' | 'environment'>('user');
  const [prompterPlaying, setPrompterPlaying] = useState(false);
  const [countdown, setCountdown] = useState(0);

  // Initialize camera
  useEffect(() => {
    let stream: MediaStream | null = null;

    async function initCamera() {
      try {
        stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode },
          audio: true
        });
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          setIsCameraActive(true);
        }
      } catch (e) {
        console.error('Camera access denied or error', e);
        alert('कैमरा और माइक्रोफोन अनुमति (Permission) दें ताकि आप रिकॉर्ड कर सकें।');
      }
    }

    initCamera();

    return () => {
      if (stream) {
        stream.getTracks().forEach((track) => track.stop());
      }
    };
  }, [facingMode]);

  // Recording timer
  useEffect(() => {
    let timer: any = null;
    if (isRecording) {
      timer = setInterval(() => {
        setRecordingSeconds((prev) => prev + 1);
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [isRecording]);

  // Teleprompter scroll inside camera
  useEffect(() => {
    let animId: any = null;
    let lastTime = performance.now();

    const loop = (currentTime: number) => {
      if (prompterPlaying && prompterBoxRef.current) {
        const delta = (currentTime - lastTime) / 1000;
        const px = (settings.scrollSpeed / 130) * 55;
        prompterBoxRef.current.scrollTop += px * delta;
      }
      lastTime = currentTime;
      if (prompterPlaying) {
        animId = requestAnimationFrame(loop);
      }
    };

    if (prompterPlaying) {
      animId = requestAnimationFrame(loop);
    }
    return () => cancelAnimationFrame(animId);
  }, [prompterPlaying, settings.scrollSpeed]);

  const startRecording = () => {
    if (!videoRef.current || !videoRef.current.srcObject) return;
    const stream = videoRef.current.srcObject as MediaStream;

    setCountdown(3);
    let c = 3;
    const countTimer = setInterval(() => {
      c -= 1;
      if (c <= 0) {
        clearInterval(countTimer);
        setCountdown(0);
        executeStartRecording(stream);
      } else {
        setCountdown(c);
      }
    }, 1000);
  };

  const executeStartRecording = (stream: MediaStream) => {
    const chunks: Blob[] = [];
    const mimeType = MediaRecorder.isTypeSupported('video/webm;codecs=vp9')
      ? 'video/webm;codecs=vp9'
      : 'video/webm';

    const recorder = new MediaRecorder(stream, { mimeType });
    recorder.ondataavailable = (e) => {
      if (e.data && e.data.size > 0) chunks.push(e.data);
    };

    recorder.onstop = () => {
      const blob = new Blob(chunks, { type: 'video/mp4' });
      const url = URL.createObjectURL(blob);
      setRecordedBlobUrl(url);
    };

    recorder.start(1000);
    mediaRecorderRef.current = recorder;
    setIsRecording(true);
    setRecordingSeconds(0);
    setPrompterPlaying(true); // Start teleprompter automatically with video
  };

  const stopRecording = () => {
    if (mediaRecorderRef.current && isRecording) {
      mediaRecorderRef.current.stop();
      setIsRecording(false);
      setPrompterPlaying(false);
    }
  };

  return (
    <div style={{ position: 'relative', width: '100%', height: 'calc(100vh - 60px)', backgroundColor: '#000000', overflow: 'hidden' }}>
      {/* Camera Live Viewfinder */}
      <video
        ref={videoRef}
        autoPlay
        playsInline
        muted
        style={{
          width: '100%',
          height: '100%',
          objectFit: 'cover',
          transform: facingMode === 'user' && settings.cameraMirror ? 'scaleX(-1)' : 'none'
        }}
      />

      {/* Top Header Bar */}
      <div style={{
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        padding: '12px 18px',
        background: 'linear-gradient(to bottom, rgba(8, 12, 20, 0.8), transparent)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        zIndex: 20
      }}>
        <button
          onClick={onBack}
          style={{ padding: '8px 12px', backgroundColor: 'rgba(15, 23, 42, 0.7)', color: '#f8fafc', border: '1px solid #334155', borderRadius: '10px', cursor: 'pointer' }}
        >
          <ChevronLeft size={18} />
        </button>

        {isRecording ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', backgroundColor: 'rgba(239, 68, 68, 0.2)', border: '1px solid #ef4444', padding: '6px 14px', borderRadius: '20px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '5px', backgroundColor: '#ef4444', animation: 'pulse 1s infinite' }} />
            <span style={{ color: '#ef4444', fontWeight: 'bold', fontSize: '13px' }}>
              REC {formatDuration(recordingSeconds)}
            </span>
          </div>
        ) : (
          <span style={{ color: '#00e5ff', fontWeight: 'bold', fontSize: '14px' }}>Camera Studio</span>
        )}

        <button
          onClick={() => setFacingMode(facingMode === 'user' ? 'environment' : 'user')}
          style={{ padding: '8px 12px', backgroundColor: 'rgba(15, 23, 42, 0.7)', color: '#f8fafc', border: '1px solid #334155', borderRadius: '10px', cursor: 'pointer' }}
          title="Flip Camera"
        >
          <RotateCcw size={18} />
        </button>
      </div>

      {/* Teleprompter Overlay directly below camera lens */}
      <div style={{
        position: 'absolute',
        top: '64px',
        left: '50%',
        transform: 'translateX(-50%)',
        width: '92%',
        maxWidth: '540px',
        height: '240px',
        backgroundColor: 'rgba(15, 23, 42, 0.82)',
        borderRadius: '16px',
        border: '1.5px solid #00e5ff',
        backdropFilter: 'blur(8px)',
        display: 'flex',
        flexDirection: 'column',
        zIndex: 20
      }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '6px 14px',
          borderBottom: '1px solid rgba(0, 229, 255, 0.2)',
          fontSize: '11px',
          color: '#00e5ff',
          fontWeight: 'bold'
        }}>
          <span>EYE-CONTACT TELEPROMPTER</span>
          <div style={{ display: 'flex', gap: '6px' }}>
            <button
              onClick={() => setPrompterPlaying(!prompterPlaying)}
              style={{ background: 'none', border: 'none', color: '#00e5ff', cursor: 'pointer', padding: '2px' }}
            >
              {prompterPlaying ? <Pause size={14} /> : <Play size={14} />}
            </button>
            <button
              onClick={() => {
                if (prompterBoxRef.current) prompterBoxRef.current.scrollTop = 0;
              }}
              style={{ background: 'none', border: 'none', color: '#f8fafc', cursor: 'pointer', padding: '2px' }}
            >
              <RotateCcw size={14} />
            </button>
          </div>
        </div>

        <div
          ref={prompterBoxRef}
          style={{
            flex: 1,
            overflowY: 'auto',
            padding: '16px',
            fontSize: `${settings.fontSizePx * 0.8}px`,
            lineHeight: 1.5,
            textAlign: settings.textAlign,
            color: '#ffffff',
            fontFamily: "'Poppins', 'Noto Sans Devanagari', sans-serif"
          }}
        >
          <div style={{ height: '40px' }} />
          {script.content}
          <div style={{ height: '180px' }} />
        </div>
      </div>

      {/* Countdown overlay */}
      {countdown > 0 && (
        <div style={{
          position: 'absolute',
          inset: 0,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: 'rgba(0,0,0,0.6)',
          zIndex: 50
        }}>
          <div style={{
            width: '120px',
            height: '120px',
            borderRadius: '60px',
            backgroundColor: '#00e5ff',
            color: '#080c14',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '64px',
            fontWeight: '900'
          }}>
            {countdown}
          </div>
        </div>
      )}

      {/* Bottom Recording Action Bar */}
      <div style={{
        position: 'absolute',
        bottom: 0,
        left: 0,
        right: 0,
        padding: '20px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '24px',
        background: 'linear-gradient(to top, rgba(8, 12, 20, 0.9), transparent)',
        zIndex: 20
      }}>
        {/* Big Circular Record Button */}
        <button
          onClick={isRecording ? stopRecording : startRecording}
          style={{
            width: '72px',
            height: '72px',
            borderRadius: '36px',
            backgroundColor: 'transparent',
            border: '4px solid #ffffff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            boxShadow: '0 4px 20px rgba(0,0,0,0.5)'
          }}
        >
          <div style={{
            width: isRecording ? '26px' : '52px',
            height: isRecording ? '26px' : '52px',
            borderRadius: isRecording ? '6px' : '26px',
            backgroundColor: '#ef4444',
            transition: 'all 0.2s ease'
          }} />
        </button>
      </div>

      {/* Video Preview Modal */}
      {recordedBlobUrl && (
        <div style={{
          position: 'fixed',
          inset: 0,
          backgroundColor: 'rgba(8, 12, 20, 0.95)',
          zIndex: 100,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '20px'
        }}>
          <div style={{ maxWidth: '480px', width: '100%', backgroundColor: '#0f172a', borderRadius: '16px', border: '1px solid #1e293b', padding: '20px' }}>
            <h3 style={{ fontSize: '18px', fontWeight: 'bold', marginBottom: '12px', color: '#00e5ff' }}>
              Recording Complete 🎉
            </h3>
            <video
              src={recordedBlobUrl}
              controls
              autoPlay
              style={{ width: '100%', maxHeight: '300px', borderRadius: '10px', backgroundColor: '#000000', marginBottom: '16px' }}
            />
            <div style={{ display: 'flex', gap: '10px' }}>
              <a
                href={recordedBlobUrl}
                download={`prompter_recording_${Date.now()}.mp4`}
                style={{
                  flex: 1,
                  padding: '12px',
                  backgroundColor: '#00e5ff',
                  color: '#080c14',
                  borderRadius: '10px',
                  textDecoration: 'none',
                  fontWeight: 'bold',
                  textAlign: 'center',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '6px'
                }}
              >
                <Download size={16} /> Download Video
              </a>
              <button
                onClick={() => setRecordedBlobUrl(null)}
                style={{
                  padding: '12px 18px',
                  backgroundColor: '#1e293b',
                  color: '#f8fafc',
                  border: '1px solid #334155',
                  borderRadius: '10px',
                  cursor: 'pointer'
                }}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

/* ====================================================================
   5. IN-APP FLOATING TELEPROMPTER SCREEN
   ==================================================================== */
function FloatingTeleprompterScreen({ script, settings, onUpdateSettings, onBack }: any) {
  const [posX, setPosX] = useState(30);
  const [posY, setPosY] = useState(90);
  const [isDragging, setIsDragging] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const [opacity, setOpacity] = useState(0.85);

  const panelRef = useRef<HTMLDivElement>(null);
  const dragStart = useRef({ x: 0, y: 0, startX: 0, startY: 0 });

  const handlePointerDown = (e: React.PointerEvent) => {
    setIsDragging(true);
    dragStart.current = {
      x: e.clientX,
      y: e.clientY,
      startX: posX,
      startY: posY
    };
    (e.target as HTMLElement).setPointerCapture(e.pointerId);
  };

  const handlePointerMove = (e: React.PointerEvent) => {
    if (!isDragging) return;
    const dx = e.clientX - dragStart.current.x;
    const dy = e.clientY - dragStart.current.y;
    setPosX(Math.max(10, Math.min(window.innerWidth - 320, dragStart.current.startX + dx)));
    setPosY(Math.max(60, Math.min(window.innerHeight - 250, dragStart.current.startY + dy)));
  };

  const handlePointerUp = () => {
    setIsDragging(false);
  };

  return (
    <div style={{ flex: 1, padding: '20px', maxWidth: '800px', margin: '0 auto', width: '100%' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
        <button
          onClick={onBack}
          style={{ padding: '8px 12px', backgroundColor: '#1e293b', color: '#f8fafc', border: '1px solid #334155', borderRadius: '10px', cursor: 'pointer' }}
        >
          <ChevronLeft size={18} />
        </button>
        <h2 style={{ fontSize: '18px', fontWeight: 'bold' }}>Floating Teleprompter</h2>
      </div>

      <div style={{ backgroundColor: '#0f172a', padding: '16px', borderRadius: '16px', border: '1px solid #1e293b', marginBottom: '20px' }}>
        <h3 style={{ color: '#00e5ff', fontSize: '15px', fontWeight: 'bold', marginBottom: '6px' }}>
          Android Native Overlay vs Web Floating Window
        </h3>
        <p style={{ color: '#94a3b8', fontSize: '13px', lineHeight: '1.6' }}>
          • <strong>Website / PWA:</strong> You can drag and position this floating widget anywhere inside this application.<br />
          • <strong>Android Native APK:</strong> Open the Android APK to float over external apps (YouTube, TikTok, Camera, Zoom) using system overlay permission!
        </p>
      </div>

      <div style={{ backgroundColor: '#1e293b', padding: '14px', borderRadius: '12px', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '14px' }}>
        <span style={{ fontSize: '13px', color: '#94a3b8' }}>Window Opacity:</span>
        <input
          type="range"
          min="0.3"
          max="1.0"
          step="0.05"
          value={opacity}
          onChange={(e) => setOpacity(Number(e.target.value))}
          style={{ flex: 1, accentColor: '#00e5ff' }}
        />
        <span style={{ color: '#00e5ff', fontWeight: 'bold', fontSize: '13px' }}>{Math.round(opacity * 100)}%</span>
      </div>

      {/* Draggable In-App Floating Panel Widget */}
      <div
        ref={panelRef}
        style={{
          position: 'fixed',
          left: `${posX}px`,
          top: `${posY}px`,
          width: '320px',
          height: '280px',
          backgroundColor: `rgba(15, 23, 42, ${opacity})`,
          borderRadius: '16px',
          border: '2px solid #00e5ff',
          boxShadow: '0 12px 36px rgba(0,0,0,0.6)',
          zIndex: 80,
          display: 'flex',
          flexDirection: 'column',
          backdropFilter: 'blur(8px)',
          overflow: 'hidden'
        }}
      >
        {/* Drag Bar */}
        <div
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          style={{
            padding: '8px 12px',
            backgroundColor: 'rgba(0, 229, 255, 0.25)',
            cursor: 'grab',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            userSelect: 'none'
          }}
        >
          <span style={{ fontSize: '12px', fontWeight: 'bold', color: '#ffffff', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '200px' }}>
            {script.title}
          </span>
          <button
            onClick={onBack}
            style={{ background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer', fontSize: '14px', fontWeight: 'bold' }}
          >
            ✕
          </button>
        </div>

        {/* Content */}
        <div style={{
          flex: 1,
          overflowY: 'auto',
          padding: '12px',
          fontSize: '18px',
          lineHeight: '1.5',
          color: '#ffffff',
          fontFamily: "'Poppins', 'Noto Sans Devanagari', sans-serif"
        }}>
          {script.content}
        </div>

        {/* Floating Controls */}
        <div style={{
          padding: '8px 12px',
          backgroundColor: '#0f172a',
          borderTop: '1px solid #1e293b',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between'
        }}>
          <button
            onClick={() => setIsPlaying(!isPlaying)}
            style={{
              padding: '6px 12px',
              backgroundColor: '#00e5ff',
              color: '#080c14',
              borderRadius: '8px',
              border: 'none',
              fontWeight: 'bold',
              fontSize: '12px',
              cursor: 'pointer'
            }}
          >
            {isPlaying ? 'Pause' : 'Play'}
          </button>
          <span style={{ fontSize: '12px', color: '#94a3b8' }}>{settings.scrollSpeed} wpm</span>
        </div>
      </div>
    </div>
  );
}

/* ====================================================================
   6. SETTINGS & GOOGLE CLOUD SYNC SCREEN
   ==================================================================== */
function SettingsScreen({ settings, onUpdateSettings, scripts }: any) {
  const [showOauthGuide, setShowOauthGuide] = useState(false);
  const [showTips, setShowTips] = useState(false);

  const handleExportAllJson = () => {
    const jsonStr = JSON.stringify(scripts, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `teleprompter_scripts_backup_${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div style={{ maxWidth: '800px', width: '100%', margin: '0 auto', padding: '20px' }}>
      <h2 style={{ fontSize: '20px', fontWeight: 'bold', marginBottom: '20px', color: '#f8fafc' }}>
        Settings & Cloud Sync
      </h2>

      {/* Cloud Sync Card */}
      <div style={{ backgroundColor: '#0f172a', padding: '18px', borderRadius: '16px', border: '1px solid #1e293b', marginBottom: '20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '10px' }}>
          <div style={{ width: '40px', height: '40px', borderRadius: '10px', backgroundColor: 'rgba(0, 229, 255, 0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#00e5ff' }}>
            <ExternalLink size={20} />
          </div>
          <div>
            <h3 style={{ fontSize: '15px', fontWeight: 'bold' }}>Google Drive Cloud Sync</h3>
            <span style={{ fontSize: '12px', color: '#f59e0b' }}>Status: Local Storage Only (Offline Safe)</span>
          </div>
        </div>

        <p style={{ color: '#94a3b8', fontSize: '13px', lineHeight: '1.6', marginBottom: '14px' }}>
          All scripts are saved safely in your browser's local IndexedDB and Android Room database. To enable cross-device cloud sync via Google Drive API, configure your Google Cloud OAuth Client ID.
        </p>

        <button
          onClick={() => setShowOauthGuide(true)}
          style={{
            padding: '10px 16px',
            backgroundColor: '#00e5ff',
            color: '#080c14',
            borderRadius: '10px',
            border: 'none',
            fontWeight: 'bold',
            fontSize: '13px',
            cursor: 'pointer'
          }}
        >
          View Google Cloud OAuth Setup Guide
        </button>
      </div>

      {/* Backup and Export */}
      <div style={{ backgroundColor: '#0f172a', padding: '18px', borderRadius: '16px', border: '1px solid #1e293b', marginBottom: '20px' }}>
        <h3 style={{ fontSize: '15px', fontWeight: 'bold', marginBottom: '8px' }}>Backup & Export</h3>
        <p style={{ color: '#94a3b8', fontSize: '13px', marginBottom: '14px' }}>
          Export all {scripts.length} scripts in a single JSON backup file for migration or offline safekeeping.
        </p>
        <button
          onClick={handleExportAllJson}
          style={{
            padding: '10px 16px',
            backgroundColor: '#1e293b',
            color: '#00e5ff',
            border: '1px solid #334155',
            borderRadius: '10px',
            fontWeight: '600',
            fontSize: '13px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '6px'
          }}
        >
          <Download size={16} /> Export All Scripts (JSON)
        </button>
      </div>

      {/* Voice Follow Language */}
      <div style={{ backgroundColor: '#0f172a', padding: '18px', borderRadius: '16px', border: '1px solid #1e293b', marginBottom: '20px' }}>
        <h3 style={{ fontSize: '15px', fontWeight: 'bold', marginBottom: '8px' }}>Voice Follow Recognition Language</h3>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            onClick={() => onUpdateSettings({ speechLanguage: 'hi-IN' })}
            style={filterChipStyle(settings.speechLanguage === 'hi-IN')}
          >
            हिन्दी (Hindi - India)
          </button>
          <button
            onClick={() => onUpdateSettings({ speechLanguage: 'en-US' })}
            style={filterChipStyle(settings.speechLanguage === 'en-US')}
          >
            English (US / Global)
          </button>
        </div>
      </div>

      {/* Creator Tips & Information */}
      <div style={{ backgroundColor: '#0f172a', padding: '18px', borderRadius: '16px', border: '1px solid #1e293b' }}>
        <h3 style={{ fontSize: '15px', fontWeight: 'bold', marginBottom: '12px' }}>Support & Information</h3>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          <button
            onClick={() => setShowTips(true)}
            style={{
              padding: '12px',
              backgroundColor: '#1e293b',
              color: '#f8fafc',
              border: 'none',
              borderRadius: '10px',
              textAlign: 'left',
              cursor: 'pointer',
              fontSize: '13px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between'
            }}
          >
            <span>📹 Eye Contact & Teleprompter Delivery Tips</span>
            <span>➔</span>
          </button>

          <div style={{ padding: '12px', backgroundColor: '#1e293b', borderRadius: '10px', fontSize: '13px', color: '#94a3b8' }}>
            <span style={{ color: '#f8fafc', fontWeight: 'bold' }}>Privacy & Security:</span> 100% offline local storage with zero analytics or tracking. Video recordings remain strictly on your device.
          </div>
        </div>
      </div>

      {/* OAuth Setup Modal */}
      {showOauthGuide && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.8)', zIndex: 120, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px' }}>
          <div style={{ maxWidth: '520px', width: '100%', backgroundColor: '#0f172a', padding: '24px', borderRadius: '16px', border: '1px solid #1e293b' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '18px', fontWeight: 'bold', color: '#00e5ff' }}>Google Drive OAuth Configuration</h3>
              <button onClick={() => setShowOauthGuide(false)} style={{ background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer', fontSize: '18px' }}>✕</button>
            </div>
            <ol style={{ color: '#94a3b8', fontSize: '13px', lineHeight: '1.8', paddingLeft: '20px', marginBottom: '20px' }}>
              <li>Visit <a href="https://console.cloud.google.com" target="_blank" rel="noreferrer" style={{ color: '#00e5ff' }}>console.cloud.google.com</a>.</li>
              <li>Create or select your project and enable <strong>Google Drive API</strong>.</li>
              <li>Under <strong>Credentials</strong>, create an OAuth 2.0 Client ID for Web / Android.</li>
              <li>Add scope <code>https://www.googleapis.com/auth/drive.file</code>.</li>
              <li>Your local IndexedDB and Room DB automatically synchronize scripts once authorized.</li>
            </ol>
            <button
              onClick={() => setShowOauthGuide(false)}
              style={{ width: '100%', padding: '12px', backgroundColor: '#00e5ff', color: '#080c14', borderRadius: '10px', border: 'none', fontWeight: 'bold', cursor: 'pointer' }}
            >
              Close Guide
            </button>
          </div>
        </div>
      )}

      {/* Tips Modal */}
      {showTips && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.8)', zIndex: 120, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px' }}>
          <div style={{ maxWidth: '520px', width: '100%', backgroundColor: '#0f172a', padding: '24px', borderRadius: '16px', border: '1px solid #1e293b' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '18px', fontWeight: 'bold', color: '#00e5ff' }}>Creator Teleprompter Tips</h3>
              <button onClick={() => setShowTips(false)} style={{ background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer', fontSize: '18px' }}>✕</button>
            </div>
            <ul style={{ color: '#94a3b8', fontSize: '13px', lineHeight: '1.8', paddingLeft: '20px', marginBottom: '20px' }}>
              <li><strong>Direct Eye Contact:</strong> Keep the teleprompter window right adjacent to the camera lens.</li>
              <li><strong>Natural Pacing:</strong> Speak at 120 - 140 WPM for YouTube videos and explanations.</li>
              <li><strong>Mirror Mode:</strong> Use horizontal flip when mounting onto physical beam-splitter teleprompter glass.</li>
              <li><strong>Voice Follow:</strong> Enable Voice Follow to let speech recognition track your spoken words automatically.</li>
            </ul>
            <button
              onClick={() => setShowTips(false)}
              style={{ width: '100%', padding: '12px', backgroundColor: '#00e5ff', color: '#080c14', borderRadius: '10px', border: 'none', fontWeight: 'bold', cursor: 'pointer' }}
            >
              Understood
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
