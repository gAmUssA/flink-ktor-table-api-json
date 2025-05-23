import { FlightEvent } from '../../models/FlightEvent';
import { DASHBOARD_CONFIG } from '../../config';

interface MetricsState {
  activeFlights: Set<string>;
  delayedFlights: Set<string>;
  densityHotspots: Map<string, number>;
  lastUpdate: Date;
}

/**
 * Initializes the metrics cards for displaying flight statistics
 * @returns Metrics controller object with methods to update the metrics
 */
export function initMetricsCards() {
  // Initialize metrics state
  const state: MetricsState = {
    activeFlights: new Set<string>(),
    delayedFlights: new Set<string>(),
    densityHotspots: new Map<string, number>(),
    lastUpdate: new Date()
  };
  
  // Get DOM elements
  const totalFlightsElement = document.querySelector('#total-flights .metric-value');
  const totalFlightsTrendElement = document.querySelector('#total-flights .metric-trend');
  
  const delayedFlightsElement = document.querySelector('#delayed-flights .metric-value');
  const delayedFlightsTrendElement = document.querySelector('#delayed-flights .metric-trend');
  
  const densityHotspotsElement = document.querySelector('#density-hotspots .metric-value');
  const densityHotspotsTrendElement = document.querySelector('#density-hotspots .metric-trend');
  
  // Function to update the UI with current metrics
  const updateUI = () => {
    // Update active flights count
    if (totalFlightsElement) {
      totalFlightsElement.textContent = state.activeFlights.size.toString();
      
      // Add animation class and remove it after animation completes
      totalFlightsElement.classList.add('pulse');
      setTimeout(() => {
        totalFlightsElement.classList.remove('pulse');
      }, 1500);
    }
    
    // Update delayed flights count
    if (delayedFlightsElement) {
      delayedFlightsElement.textContent = state.delayedFlights.size.toString();
      
      // Add animation class and remove it after animation completes
      if (state.delayedFlights.size > 0) {
        delayedFlightsElement.classList.add('pulse');
        setTimeout(() => {
          delayedFlightsElement.classList.remove('pulse');
        }, 1500);
      }
    }
    
    // Update density hotspots count
    if (densityHotspotsElement) {
      // Count grid cells with more than 3 flights
      const hotspots = Array.from(state.densityHotspots.values()).filter(count => count >= 3).length;
      densityHotspotsElement.textContent = hotspots.toString();
    }
    
    // Update trend indicators (this would be more sophisticated in a real app)
    if (totalFlightsTrendElement) {
      const trend = state.activeFlights.size > 10 ? '+10%' : state.activeFlights.size > 5 ? '+5%' : '0%';
      totalFlightsTrendElement.textContent = `${trend} from average`;
      
      if (state.activeFlights.size > 10) {
        totalFlightsTrendElement.className = 'metric-trend up';
      } else if (state.activeFlights.size > 5) {
        totalFlightsTrendElement.className = 'metric-trend up';
      } else {
        totalFlightsTrendElement.className = 'metric-trend neutral';
      }
    }
    
    if (delayedFlightsTrendElement) {
      const delayedPercentage = state.activeFlights.size > 0 
        ? Math.round((state.delayedFlights.size / state.activeFlights.size) * 100) 
        : 0;
      
      delayedFlightsTrendElement.textContent = `${delayedPercentage}% of flights`;
      
      if (delayedPercentage > 20) {
        delayedFlightsTrendElement.className = 'metric-trend up';
      } else if (delayedPercentage > 10) {
        delayedFlightsTrendElement.className = 'metric-trend neutral';
      } else {
        delayedFlightsTrendElement.className = 'metric-trend down';
      }
    }
  };
  
  // Function to clean up old flights (flights that haven't been updated in a while)
  const cleanupOldFlights = () => {
    const now = new Date();
    const maxAge = 5 * 60 * 1000; // 5 minutes
    
    // We would need to track last update time for each flight in a real implementation
    // For this demo, we'll just reset the counts periodically
    if (now.getTime() - state.lastUpdate.getTime() > maxAge) {
      state.activeFlights.clear();
      state.delayedFlights.clear();
      state.densityHotspots.clear();
      state.lastUpdate = now;
      updateUI();
    }
  };
  
  // Start cleanup interval
  setInterval(cleanupOldFlights, 60 * 1000); // Run every minute
  
  // Return metrics controller object
  return {
    /**
     * Updates metrics based on a flight event
     * @param event Flight event data
     */
    updateMetrics: (event: FlightEvent) => {
      // Update active flights
      state.activeFlights.add(event.flightId);
      
      // Update delayed flights
      if (event.delayMinutes > 15) {
        state.delayedFlights.add(event.flightId);
      } else {
        state.delayedFlights.delete(event.flightId);
      }
      
      // Update density hotspots (simplified grid-based approach)
      const gridKey = `${Math.floor(event.latitude)}:${Math.floor(event.longitude)}`;
      const currentCount = state.densityHotspots.get(gridKey) || 0;
      state.densityHotspots.set(gridKey, currentCount + 1);
      
      // Update the UI
      updateUI();
    },
    
    /**
     * Gets the current metrics state
     * @returns Current metrics state
     */
    getMetricsState: () => {
      return {
        activeFlights: state.activeFlights.size,
        delayedFlights: state.delayedFlights.size,
        densityHotspots: Array.from(state.densityHotspots.values()).filter(count => count >= 3).length
      };
    }
  };
}
