import './styles/main.css';
import { initMap } from './components/Map/Map';
import { initMetricsCards } from './components/MetricsCard/MetricsCards';
import { initAlertFeed } from './components/AlertFeed/AlertFeed';
import { setupWebSocket } from './services/WebSocketService';
import { apiService } from './services/ApiService';
import { convertDelayedFlightsToAlerts, createDelayedFlightsPanel } from './components/AlertFeed/DelayedFlights';
import { FlightEvent } from './models/FlightEvent';
import { DASHBOARD_CONFIG } from './config';

// Initialize the dashboard components
document.addEventListener('DOMContentLoaded', () => {
  console.log('Flight Control Dashboard initializing...');
  
  // Initialize the map
  const map = initMap('map');
  
  // Initialize metrics cards
  const metricsCards = initMetricsCards();
  
  // Initialize alert feed
  const alertFeed = initAlertFeed('alerts-feed');
  
  // Create delayed flights panel element
  const delayedFlightsContainer = document.getElementById('delayed-flights-container');
  
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
  
  // Function to fetch and update flight density data
  const updateFlightDensity = async () => {
    try {
      const densityResponse = await apiService.getFlightDensity();
      if (densityResponse && densityResponse.data) {
        // Update map with density data
        map.updateFlightDensity(densityResponse.data);
        
        // Update metrics with density data
        const totalFlights = densityResponse.data.reduce((sum, item) => sum + item.flight_count, 0);
        metricsCards.updateFlightCount(totalFlights);
        
        console.log(`Updated flight density data: ${densityResponse.data.length} grid cells`);
      }
    } catch (error) {
      console.error('Error fetching flight density data:', error);
    }
  };
  
  // Function to fetch and update delayed flights data
  const updateDelayedFlights = async () => {
    try {
      const delayedResponse = await apiService.getDelayedFlights();
      if (delayedResponse && delayedResponse.data) {
        // Convert delayed flights to alerts
        const alerts = convertDelayedFlightsToAlerts(delayedResponse.data);
        
        // Add alerts to the feed
        alerts.forEach(alert => alertFeed.addAlert(alert));
        
        // Update delayed flights panel if it exists
        if (delayedFlightsContainer) {
          delayedFlightsContainer.innerHTML = createDelayedFlightsPanel(delayedResponse.data);
        }
        
        // Update metrics with delayed flights count
        metricsCards.updateDelayedCount(delayedResponse.data.length);
        
        console.log(`Updated delayed flights data: ${delayedResponse.data.length} flights`);
      }
    } catch (error) {
      console.error('Error fetching delayed flights data:', error);
    }
  };
  
  // Initial data fetch
  updateFlightDensity();
  updateDelayedFlights();
  
  // Set up periodic updates
  setInterval(updateFlightDensity, DASHBOARD_CONFIG.UPDATE_INTERVAL * 5); // Less frequent updates for density
  setInterval(updateDelayedFlights, DASHBOARD_CONFIG.UPDATE_INTERVAL * 3); // More frequent updates for delays
  
  console.log('Flight Control Dashboard initialized successfully');
});
