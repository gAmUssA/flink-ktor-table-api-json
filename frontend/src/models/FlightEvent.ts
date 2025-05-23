/**
 * Interface representing a flight event from the backend API
 */
export interface FlightEvent {
  flightId: string;
  airline: string;
  eventType: 'POSITION_UPDATE' | 'TAKEOFF' | 'LANDING' | 'DELAY_UPDATE';
  timestamp: string;
  latitude: number;
  longitude: number;
  delayMinutes: number;
  origin: string;
  destination: string;
}

/**
 * Interface for alert items displayed in the alert feed
 */
export interface AlertItem {
  title: string;
  content: string;
  type: 'info' | 'success' | 'warning' | 'danger';
  timestamp: Date;
}
