import './styles/main.css';
import { initMap } from './components/Map/Map';
import { initMetricsCards } from './components/MetricsCard/MetricsCards';
import { initAlertFeed } from './components/AlertFeed/AlertFeed';
import { setupWebSocket } from './services/WebSocketService';
import { FlightEvent } from './models/FlightEvent';

// Initialize the dashboard components
document.addEventListener('DOMContentLoaded', () => {
  console.log('Flight Control Dashboard initializing...');
  
  // Initialize the map
  const map = initMap('map');
  
  // Initialize metrics cards
  const metricsCards = initMetricsCards();
  
  // Initialize alert feed
  const alertFeed = initAlertFeed('alerts-feed');
  
  // Setup WebSocket connection
  const ws = setupWebSocket((event: FlightEvent) => {
    // Update map with flight position
    map.updateFlightPosition(event);
    
    // Update metrics based on event data
    metricsCards.updateMetrics(event);
    
    // Add alert if needed (e.g., for delays or important events)
    if (event.eventType === 'DELAY_UPDATE' || event.delayMinutes > 15) {
      alertFeed.addAlert({
        title: `Flight ${event.flightId} Delayed`,
        content: `${event.airline} flight from ${event.origin} to ${event.destination} is delayed by ${event.delayMinutes} minutes.`,
        type: event.delayMinutes > 30 ? 'danger' : 'warning',
        timestamp: new Date(event.timestamp)
      });
    } else if (event.eventType === 'TAKEOFF') {
      alertFeed.addAlert({
        title: `Flight ${event.flightId} Departed`,
        content: `${event.airline} flight from ${event.origin} to ${event.destination} has taken off.`,
        type: 'info',
        timestamp: new Date(event.timestamp)
      });
    } else if (event.eventType === 'LANDING') {
      alertFeed.addAlert({
        title: `Flight ${event.flightId} Arrived`,
        content: `${event.airline} flight from ${event.origin} to ${event.destination} has landed.`,
        type: 'success',
        timestamp: new Date(event.timestamp)
      });
    }
  });
  
  // Update current time display
  const timeDisplay = document.getElementById('current-time');
  if (timeDisplay) {
    const updateTime = () => {
      const now = new Date();
      timeDisplay.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    };
    updateTime();
    setInterval(updateTime, 1000);
  }
  
  // Handle connection status
  const statusDot = document.querySelector('.status-dot');
  const statusText = document.querySelector('.status-text');
  
  window.addEventListener('online', () => {
    if (statusDot && statusText) {
      statusDot.classList.remove('offline');
      statusDot.classList.add('online');
      statusText.textContent = 'Connected';
    }
  });
  
  window.addEventListener('offline', () => {
    if (statusDot && statusText) {
      statusDot.classList.remove('online');
      statusDot.classList.add('offline');
      statusText.textContent = 'Disconnected';
    }
  });
  
  // Handle map controls
  const zoomInBtn = document.getElementById('zoom-in');
  const zoomOutBtn = document.getElementById('zoom-out');
  const centerMapBtn = document.getElementById('center-map');
  
  if (zoomInBtn) {
    zoomInBtn.addEventListener('click', () => map.zoomIn());
  }
  
  if (zoomOutBtn) {
    zoomOutBtn.addEventListener('click', () => map.zoomOut());
  }
  
  if (centerMapBtn) {
    centerMapBtn.addEventListener('click', () => map.centerMap());
  }
  
  // Handle clear alerts button
  const clearAlertsBtn = document.getElementById('clear-alerts');
  if (clearAlertsBtn) {
    clearAlertsBtn.addEventListener('click', () => alertFeed.clearAlerts());
  }
  
  console.log('Flight Control Dashboard initialized successfully');
});
