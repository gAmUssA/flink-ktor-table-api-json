import { DelayedFlight } from '../../services/ApiService';
import { AlertItem } from '../../models/FlightEvent';

/**
 * Converts delayed flight data to alert items for display in the alert feed
 * @param delayedFlights Array of delayed flight data
 * @returns Array of alert items
 */
export function convertDelayedFlightsToAlerts(delayedFlights: DelayedFlight[]): AlertItem[] {
  return delayedFlights.map(flight => {
    // Determine alert type based on delay severity
    let alertType: 'info' | 'warning' | 'danger';
    if (flight.delayMinutes < 30) {
      alertType = 'info';
    } else if (flight.delayMinutes < 60) {
      alertType = 'warning';
    } else {
      alertType = 'danger';
    }
    
    // Create alert title
    const title = `Flight ${flight.flightId} Delayed`;
    
    // Create alert content
    const content = `${flight.airline} flight from ${flight.origin} to ${flight.destination} is delayed by ${flight.delayMinutes} minutes.`;
    
    // Return alert item
    return {
      title,
      content,
      type: alertType,
      timestamp: new Date(flight.timestamp)
    };
  });
}

/**
 * Creates HTML for the delayed flights panel
 * @param delayedFlights Array of delayed flight data
 * @returns HTML string for the delayed flights panel
 */
export function createDelayedFlightsPanel(delayedFlights: DelayedFlight[]): string {
  if (delayedFlights.length === 0) {
    return '<div class="no-delays">No delayed flights at this time</div>';
  }
  
  // Sort flights by delay time (most delayed first)
  const sortedFlights = [...delayedFlights].sort((a, b) => b.delayMinutes - a.delayMinutes);
  
  // Create HTML for each flight
  const flightItems = sortedFlights.map(flight => {
    // Determine severity class
    let severityClass = '';
    if (flight.delayMinutes >= 60) {
      severityClass = 'severe-delay';
    } else if (flight.delayMinutes >= 30) {
      severityClass = 'moderate-delay';
    } else {
      severityClass = 'minor-delay';
    }
    
    // Format timestamp
    const timestamp = new Date(flight.timestamp).toLocaleTimeString();
    
    return `
      <div class="delayed-flight-item ${severityClass}">
        <div class="flight-header">
          <span class="flight-id">${flight.flightId}</span>
          <span class="airline">${flight.airline}</span>
        </div>
        <div class="flight-route">${flight.origin} → ${flight.destination}</div>
        <div class="delay-info">
          <span class="delay-time">Delayed by ${flight.delayMinutes} minutes</span>
          <span class="timestamp">Updated at ${timestamp}</span>
        </div>
      </div>
    `;
  }).join('');
  
  // Return complete panel HTML
  return `
    <div class="delayed-flights-panel">
      <h3>Delayed Flights (${sortedFlights.length})</h3>
      <div class="delayed-flights-list">
        ${flightItems}
      </div>
    </div>
  `;
}
