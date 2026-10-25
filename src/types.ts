export interface Script {
  id: number;
  title: string;
  content: string;
  language: 'hi' | 'en' | 'mixed';
  wordCount: number;
  estimatedDurationSec: number;
  updatedAt: number;
  createdAt: number;
  isFavorite: boolean;
}

export type CameraFilter = 'none' | 'cool' | 'warm' | 'studio' | 'mono';

export interface PrompterSettings {
  scrollSpeed: number; // wpm (40 - 320)
  fontSizePx: number; // 20px - 60px
  textColor: string;
  backgroundColor: string;
  backgroundOpacity: number;
  textAlign: 'left' | 'center' | 'right';
  mirrorMode: boolean;
  countdownSeconds: number;
  voiceFollowEnabled: boolean;
  speechLanguage: string; // 'hi-IN' | 'en-US'
  cameraFacing: 'user' | 'environment';
  cameraMirror: boolean;
  cameraFilter: CameraFilter;
}
