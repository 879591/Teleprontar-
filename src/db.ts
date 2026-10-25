import { Script } from './types';

const DB_NAME = 'TeleprompterDB';
const DB_VERSION = 1;
const STORE_NAME = 'scripts';

const INITIAL_SCRIPTS: Omit<Script, 'id'>[] = [
  {
    title: 'हिंदी वीडियो स्क्रिप्ट - कैमरा प्रजेंस',
    content: `नमस्ते दोस्तों! आज के इस विशेष वीडियो में आपका हार्दिक स्वागत है।
आज हम बात करने वाले हैं डिजिटल क्रिएशन और कैमरा प्रेजेंस के बारे में।
जब आप कैमरे के सामने बोलते हैं, तो सबसे महत्वपूर्ण बात है आपका आत्मविश्वास और सीधा आई-कॉन्टैक्ट।
एक अच्छे टेलीप्रॉम्प्टर के साथ आप बिना झिझके, बिना रुके अपने विचारों को सीधे दर्शकों तक पहुँचा सकते हैं।
अपने बोलने की गति को सामान्य रखें और चेहरे पर एक हल्की मुस्कान बनाए रखें।
यदि आपको यह वीडियो उपयोगी लगे, तो लाइक करें और अपने साथी क्रिएटर्स के साथ शेयर करना न भूलें।
धन्यवाद, जय हिन्द!`,
    language: 'hi',
    wordCount: 88,
    estimatedDurationSec: 41,
    updatedAt: Date.now(),
    createdAt: Date.now(),
    isFavorite: true
  },
  {
    title: 'Mastering Eye Contact & Delivery',
    content: `Welcome creators to this masterclass on confident video delivery!
Whether you are recording YouTube shorts, educational courses, or keynote speeches, eye contact is everything.
By positioning your script right beside the camera lens, you speak naturally while keeping viewers engaged.
Remember to breathe at natural sentence pauses, vary your tone, and let your natural enthusiasm shine through.
Let's make creating high-quality content effortless and fun!`,
    language: 'en',
    wordCount: 68,
    estimatedDurationSec: 32,
    updatedAt: Date.now() - 3600000,
    createdAt: Date.now() - 3600000,
    isFavorite: true
  },
  {
    title: 'Tech Review (हिंदी + English Mixed)',
    content: `Hindi-English Tech Review (टेक रिव्यू)
दोस्तों, आज हम अनबॉक्स करने वाले हैं नए स्मार्टफोन को।
इस फोन का कैमरा 4K 60FPS वीडियो रिकॉर्डिंग सपोर्ट करता है और बैटरी लाइफ पूरे दिन चलती है।
Display is stunning with 120Hz refresh rate and AMOLED clarity.
गेमिंग परफॉरमेंस भी काफी स्मूथ है।
Let me know your thoughts in the comments below!`,
    language: 'mixed',
    wordCount: 56,
    estimatedDurationSec: 26,
    updatedAt: Date.now() - 7200000,
    createdAt: Date.now() - 7200000,
    isFavorite: false
  }
];

export function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);

    request.onupgradeneeded = (event) => {
      const db = (event.target as IDBOpenDBRequest).result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        const store = db.createObjectStore(STORE_NAME, { keyPath: 'id', autoIncrement: true });
        INITIAL_SCRIPTS.forEach((script) => store.add(script));
      }
    };

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

export async function getAllScripts(): Promise<Script[]> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(STORE_NAME, 'readonly');
    const store = transaction.objectStore(STORE_NAME);
    const request = store.getAll();
    request.onsuccess = () => {
      const list = request.result as Script[];
      list.sort((a, b) => b.updatedAt - a.updatedAt);
      resolve(list);
    };
    request.onerror = () => reject(request.error);
  });
}

export async function saveScript(script: Partial<Script>): Promise<number> {
  const db = await openDB();
  const wordCount = calculateWordCount(script.content || '');
  const duration = calculateDurationSec(wordCount);
  
  const payload: any = {
    ...script,
    wordCount,
    estimatedDurationSec: duration,
    updatedAt: Date.now(),
    createdAt: script.createdAt || Date.now(),
    isFavorite: script.isFavorite || false
  };

  return new Promise((resolve, reject) => {
    const transaction = db.transaction(STORE_NAME, 'readwrite');
    const store = transaction.objectStore(STORE_NAME);
    const request = store.put(payload);
    request.onsuccess = () => resolve(Number(request.result));
    request.onerror = () => reject(request.error);
  });
}

export async function deleteScript(id: number): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(STORE_NAME, 'readwrite');
    const store = transaction.objectStore(STORE_NAME);
    const request = store.delete(id);
    request.onsuccess = () => resolve();
    request.onerror = () => reject(request.error);
  });
}

export function calculateWordCount(text: string): number {
  if (!text || !text.trim()) return 0;
  return text.trim().split(/\s+/).filter(Boolean).length;
}

export function calculateDurationSec(wordCount: number, wpm: number = 130): number {
  if (wordCount <= 0 || wpm <= 0) return 0;
  return Math.round((wordCount * 60) / wpm);
}

export function formatDuration(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
}
