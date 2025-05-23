import * as L from 'leaflet';
import { FlightEvent } from '../../models/FlightEvent';
import { DASHBOARD_CONFIG } from '../../config';
import { FlightDensity } from '../../services/ApiService';

interface FlightMarker {
  marker: L.Marker;
  lastUpdate: Date;
  flightId: string;
}

interface DensityLayer {
  layer: L.Layer;
  timestamp: Date;
}

/**
 * Initializes the map component for the flight control dashboard
 * @param elementId ID of the HTML element to render the map in
 * @returns Map controller object with methods to interact with the map
 */
export function initMap(elementId: string) {
  // Flight markers collection
  const flightMarkers: Record<string, FlightMarker> = {};
  
  // Density layer reference
  let densityLayer: DensityLayer | null = null;
  
  // Create map instance with default center from config
  const map = L.map(elementId, {
    center: [DASHBOARD_CONFIG.MAP_CENTER.LAT, DASHBOARD_CONFIG.MAP_CENTER.LNG],
    zoom: DASHBOARD_CONFIG.MAP_ZOOM,
    zoomControl: false, // We'll use our custom controls
  });
  
  // Store user location if available
  let userLocation: [number, number] | null = null;
  let userLocationMarker: L.Marker | null = null;
  
  // Try to get user's location and center map accordingly
  if (navigator.geolocation) {
    console.log('Geolocation is supported, attempting to get user location...');
    
    navigator.geolocation.getCurrentPosition(
      // Success callback
      (position) => {
        const { latitude, longitude } = position.coords;
        console.log(`User location obtained: ${latitude}, ${longitude}`);
        
        // Store user location
        userLocation = [latitude, longitude];
        
        // Center map on user's location
        map.setView(userLocation, DASHBOARD_CONFIG.MAP_ZOOM);
        
        // Add a marker for user's location
        userLocationMarker = L.marker(userLocation, {
          icon: L.divIcon({
            className: 'user-location-marker',
            html: '<div class="pulse"></div>',
            iconSize: [20, 20],
            iconAnchor: [10, 10]
          })
        })
        .addTo(map)
        .bindPopup('Your Location')
        .openPopup();
      },
      // Error callback
      (error) => {
        console.warn(`Geolocation error (${error.code}): ${error.message}`);
        // Fall back to default center from config
      },
      // Options
      {
        enableHighAccuracy: false, // No need for high accuracy
        timeout: 5000,            // 5 second timeout
        maximumAge: 0              // Don't use cached position
      }
    );
  } else {
    console.log('Geolocation is not supported by this browser, using default map center');
  }
  
  // Add tile layer (map background)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    maxZoom: 19,
  }).addTo(map);
  
  // Create custom flight icon
  const flightIcon = L.icon({
    iconUrl: 'https://cdn-icons-png.flaticon.com/512/61/61212.png',
    iconSize: [24, 24],
    iconAnchor: [12, 12],
    popupAnchor: [0, -12],
  });
  
  // Create delayed flight icon (red)
  const delayedFlightIcon = L.icon({
    iconUrl: 'https://cdn-icons-png.flaticon.com/512/61/61212.png',
    iconSize: [24, 24],
    iconAnchor: [12, 12],
    popupAnchor: [0, -12],
    className: 'delayed-flight-icon',
  });
  
  // Function to clean up old markers (flights that haven't been updated in a while)
  const cleanupOldMarkers = () => {
    const now = new Date();
    const maxAge = 5 * 60 * 1000; // 5 minutes
    
    Object.keys(flightMarkers).forEach(flightId => {
      const marker = flightMarkers[flightId];
      const age = now.getTime() - marker.lastUpdate.getTime();
      
      if (age > maxAge) {
        map.removeLayer(marker.marker);
        delete flightMarkers[flightId];
      }
    });
  };
  
  // Start cleanup interval
  setInterval(cleanupOldMarkers, 60 * 1000); // Run every minute
  
  // Return map controller object
  return {
    /**
     * Updates the position of a flight on the map
     * @param event Flight event with position data
     */
    updateFlightPosition: (event: FlightEvent) => {
      const position: L.LatLngExpression = [event.latitude, event.longitude];
      const isDelayed = event.delayMinutes > 15;
      const icon = isDelayed ? delayedFlightIcon : flightIcon;
      
      // Create popup content
      const popupContent = `
        <div class="flight-popup">
          <div class="flight-popup-header">
            <strong>${event.flightId}</strong>
            <span>${event.airline}</span>
          </div>
          <div class="flight-popup-content">
            <div>Route: ${event.origin} → ${event.destination}</div>
            ${isDelayed ? `<div class="delayed">Delayed: ${event.delayMinutes} minutes</div>` : ''}
            <div>Last update: ${new Date(event.timestamp).toLocaleTimeString()}</div>
          </div>
        </div>
      `;
      
      if (flightMarkers[event.flightId]) {
        // Update existing marker
        const marker = flightMarkers[event.flightId].marker;
        marker.setLatLng(position);
        marker.setIcon(icon);
        marker.getPopup()?.setContent(popupContent);
        
        // Update last update time
        flightMarkers[event.flightId].lastUpdate = new Date();
      } else {
        // Create new marker
        const marker = L.marker(position, { icon })
          .addTo(map)
          .bindPopup(popupContent);
        
        // Add to collection
        flightMarkers[event.flightId] = {
          marker,
          lastUpdate: new Date(),
          flightId: event.flightId,
        };
      }
    },
    
    /**
     * Zooms in on the map
     */
    zoomIn: () => {
      map.zoomIn();
    },
    
    /**
     * Zooms out on the map
     */
    zoomOut: () => {
      map.zoomOut();
    },
    
    /**
     * Centers the map on user's location if available, otherwise on default center
     */
    centerMap: () => {
      if (userLocation) {
        // If user location is available, center on it
        map.setView(userLocation, DASHBOARD_CONFIG.MAP_ZOOM);
        // Highlight the user location marker
        if (userLocationMarker) {
          userLocationMarker.openPopup();
        }
      } else {
        // Otherwise use the default center from config
        map.setView(
          [DASHBOARD_CONFIG.MAP_CENTER.LAT, DASHBOARD_CONFIG.MAP_CENTER.LNG], 
          DASHBOARD_CONFIG.MAP_ZOOM
        );
      }
    },
    
    /**
     * Updates the flight density heatmap on the map
     * @param densityData Array of flight density data points
     */
    updateFlightDensity: (densityData: FlightDensity[]) => {
      // Remove existing density layer if it exists
      if (densityLayer) {
        map.removeLayer(densityLayer.layer);
      }
      
      // Create heatmap data points from density data
      const heatmapPoints = densityData.map(point => {
        // Convert grid coordinates to actual lat/lon
        // Using a simple conversion for demonstration
        return [
          point.grid_lat, 
          point.grid_lon, 
          point.flight_count * 5 // Multiply by 5 to make the heatmap more visible
        ];
      });
      
      // Create new heatmap layer
      if (heatmapPoints.length > 0) {
        // @ts-ignore - Leaflet heatmap plugin type definition issue
        const heatLayer = L.heatLayer(heatmapPoints, {
          radius: 25,
          blur: 15,
          maxZoom: 10,
          max: 100,
          gradient: { 0.4: 'blue', 0.6: 'cyan', 0.7: 'lime', 0.8: 'yellow', 1.0: 'red' }
        }).addTo(map);
        
        // Store reference to the layer
        densityLayer = {
          layer: heatLayer,
          timestamp: new Date()
        };
      }
    },
    
    /**
     * Gets the raw Leaflet map instance
     * @returns Leaflet map instance
     */
    getMapInstance: () => {
      return map;
    }
  };
}
