import { FlightEvent } from '../models/FlightEvent';
import { API_CONFIG } from '../config';

/**
 * Sets up a WebSocket connection to the backend API for real-time flight data
 * @param onMessage Callback function to handle incoming flight events
 * @returns Object with methods to control the WebSocket connection
 */
export function setupWebSocket(onMessage: (event: FlightEvent) => void) {
  let socket: WebSocket | null = null;
  let reconnectTimeout: number | null = null;
  let reconnectAttempts = 0;
  const maxReconnectAttempts = 5;
  const reconnectDelay = 2000; // 2 seconds
  
  const connect = () => {
    // Close existing socket if it exists
    if (socket) {
      socket.close();
    }
    
    // Create new WebSocket connection
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    
    // Use configurable API base URL and endpoint from config
    const baseUrl = API_CONFIG.BASE_URL.replace(/^http/, 'ws');
    const endpoint = API_CONFIG.WS.FLIGHTS_LIVE;
    const wsUrl = `${baseUrl}${endpoint}`;
    
    console.log(`Connecting to WebSocket at ${wsUrl}`);
    socket = new WebSocket(wsUrl);
    
    socket.onopen = () => {
      console.log('WebSocket connection established');
      reconnectAttempts = 0;
      
      // Update UI to show connected status
      const statusDot = document.querySelector('.status-dot');
      const statusText = document.querySelector('.status-text');
      
      if (statusDot && statusText) {
        statusDot.classList.remove('offline');
        statusDot.classList.add('online');
        statusText.textContent = 'Connected';
      }
    };
    
    socket.onmessage = (event) => {
      try {
        const flightEvent = JSON.parse(event.data) as FlightEvent;
        onMessage(flightEvent);
      } catch (error) {
        console.error('Error parsing WebSocket message:', error);
      }
    };
    
    socket.onclose = (event) => {
      console.log(`WebSocket connection closed: ${event.code} ${event.reason}`);
      
      // Update UI to show disconnected status
      const statusDot = document.querySelector('.status-dot');
      const statusText = document.querySelector('.status-text');
      
      if (statusDot && statusText) {
        statusDot.classList.remove('online');
        statusDot.classList.add('offline');
        statusText.textContent = 'Disconnected';
      }
      
      // Attempt to reconnect if not closed intentionally
      if (!event.wasClean && reconnectAttempts < maxReconnectAttempts) {
        reconnectAttempts++;
        console.log(`Attempting to reconnect (${reconnectAttempts}/${maxReconnectAttempts})...`);
        
        if (reconnectTimeout) {
          window.clearTimeout(reconnectTimeout);
        }
        
        reconnectTimeout = window.setTimeout(() => {
          connect();
        }, reconnectDelay);
      }
    };
    
    socket.onerror = (error) => {
      console.error('WebSocket error:', error);
    };
  };
  
  // Initial connection
  connect();
  
  // Return methods to control the WebSocket
  return {
    disconnect: () => {
      if (socket) {
        socket.close(1000, 'Closed by user');
      }
      
      if (reconnectTimeout) {
        window.clearTimeout(reconnectTimeout);
        reconnectTimeout = null;
      }
    },
    
    reconnect: () => {
      reconnectAttempts = 0;
      connect();
    },
    
    isConnected: () => {
      return socket && socket.readyState === WebSocket.OPEN;
    }
  };
}
