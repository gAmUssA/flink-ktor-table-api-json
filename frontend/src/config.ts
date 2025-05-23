/**
 * Application configuration
 * 
 * This file contains configuration values that can be overridden
 * via environment variables at build time.
 */

// API configuration
export const API_CONFIG = {
  // Base URL for the API server (without trailing slash)
  BASE_URL: process.env.REACT_APP_API_BASE_URL || 'http://localhost:8090',
  
  // WebSocket endpoints
  WS: {
    // Flight live updates endpoint
    FLIGHTS_LIVE: process.env.REACT_APP_WS_FLIGHTS_LIVE || '/api/flights/live',
  },
  
  // REST API endpoints
  REST: {
    // Flight density endpoint
    FLIGHTS_DENSITY: process.env.REACT_APP_API_FLIGHTS_DENSITY || '/api/flights/density',
    
    // Delayed flights endpoint
    FLIGHTS_DELAYED: process.env.REACT_APP_API_FLIGHTS_DELAYED || '/api/flights/delayed',
  }
};

// Dashboard configuration
export const DASHBOARD_CONFIG = {
  // Update interval for metrics (in milliseconds)
  UPDATE_INTERVAL: parseInt(process.env.REACT_APP_UPDATE_INTERVAL || '1000', 10),
  
  // Maximum number of alerts to display
  MAX_ALERTS: parseInt(process.env.REACT_APP_MAX_ALERTS || '20', 10),
  
  // Initial map center coordinates
  MAP_CENTER: {
    LAT: parseFloat(process.env.REACT_APP_MAP_CENTER_LAT || '50.0'),
    LNG: parseFloat(process.env.REACT_APP_MAP_CENTER_LNG || '10.0'),
  },
  
  // Initial map zoom level
  MAP_ZOOM: parseInt(process.env.REACT_APP_MAP_ZOOM || '5', 10),
};
