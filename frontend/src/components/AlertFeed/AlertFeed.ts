import { AlertItem } from '../../models/FlightEvent';
import { DASHBOARD_CONFIG } from '../../config';

/**
 * Initializes the alert feed component for displaying flight alerts
 * @param elementId ID of the HTML element to render the alerts in
 * @returns Alert feed controller object with methods to add and manage alerts
 */
export function initAlertFeed(elementId: string) {
  // Get the container element
  const container = document.getElementById(elementId);
  if (!container) {
    console.error(`Alert feed container with ID "${elementId}" not found`);
    return {
      addAlert: () => {},
      clearAlerts: () => {}
    };
  }
  
  // Maximum number of alerts to display from configuration
  const maxAlerts = DASHBOARD_CONFIG.MAX_ALERTS;
  
  // Array to store alerts
  const alerts: AlertItem[] = [];
  
  // Flag to track if a render update is pending
  let updatePending = false;
  
  // Interval for refreshing the display (2 seconds)
  const REFRESH_INTERVAL = 2000;
  
  /**
   * Creates an HTML element for an alert
   * @param alert Alert data
   * @returns HTML element for the alert
   */
  const createAlertElement = (alert: AlertItem): HTMLElement => {
    const alertElement = document.createElement('div');
    alertElement.className = `alert-item ${alert.type}`;
    
    const alertHeader = document.createElement('div');
    alertHeader.className = 'alert-header';
    
    const alertTitle = document.createElement('div');
    alertTitle.className = 'alert-title';
    alertTitle.textContent = alert.title;
    
    const alertTime = document.createElement('div');
    alertTime.className = 'alert-time';
    alertTime.textContent = alert.timestamp.toLocaleTimeString([], { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
    
    alertHeader.appendChild(alertTitle);
    alertHeader.appendChild(alertTime);
    
    const alertContent = document.createElement('div');
    alertContent.className = 'alert-content';
    alertContent.textContent = alert.content;
    
    alertElement.appendChild(alertHeader);
    alertElement.appendChild(alertContent);
    
    // Add click handler to dismiss the alert
    alertElement.addEventListener('click', () => {
      alertElement.style.opacity = '0';
      setTimeout(() => {
        alertElement.remove();
        const index = alerts.findIndex(a => 
          a.title === alert.title && 
          a.timestamp === alert.timestamp
        );
        if (index !== -1) {
          alerts.splice(index, 1);
        }
      }, 300);
    });
    
    return alertElement;
  };
  
  /**
   * Renders all alerts in the container
   */
  const renderAlerts = () => {
    // Clear the container
    container.innerHTML = '';
    
    // Add alerts in reverse chronological order (newest first)
    alerts
      .sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime())
      .slice(0, DASHBOARD_CONFIG.MAX_ALERTS) // Ensure we only show the max number of alerts
      .forEach(alert => {
        container.appendChild(createAlertElement(alert));
      });
    
    // Reset the update pending flag
    updatePending = false;
  };
  
  /**
   * Schedule a render update if one is not already pending
   */
  const scheduleUpdate = () => {
    if (!updatePending) {
      updatePending = true;
      setTimeout(renderAlerts, REFRESH_INTERVAL);
    }
  };
  
  // Return alert feed controller object
  return {
    /**
     * Adds a new alert to the feed
     * @param alert Alert data
     */
    addAlert: (alert: AlertItem) => {
      // Add the alert to the array
      alerts.unshift(alert);
      
      // Limit the number of alerts
      if (alerts.length > maxAlerts) {
        alerts.pop();
      }
      
      // Schedule an update to refresh the display
      scheduleUpdate();
      
      // Play notification sound (if we had one)
      // new Audio('/path/to/notification.mp3').play().catch(e => console.log('Audio play failed:', e));
    },
    
    /**
     * Clears all alerts from the feed
     */
    clearAlerts: () => {
      // Clear the alerts array
      alerts.length = 0;
      
      // Immediately render the empty state instead of waiting for the next refresh
      // This provides better user feedback that the clear action worked
      updatePending = false; // Reset the pending flag
      
      // Animate the removal of alerts
      const alertElements = container.querySelectorAll('.alert-item');
      alertElements.forEach((element, index) => {
        setTimeout(() => {
          (element as HTMLElement).style.opacity = '0';
          setTimeout(() => element.remove(), 300);
        }, index * 50); // Stagger the animations
      });
    }
  };
}
