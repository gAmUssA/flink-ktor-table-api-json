import { API_CONFIG } from '../config';

/**
 * Interface for flight density data from the API
 */
export interface FlightDensity {
  grid_lat: number;
  grid_lon: number;
  flight_count: number;
  window_start: string;
}

/**
 * Interface for delayed flight data from the API
 */
export interface DelayedFlight {
  flightId: string;
  airline: string;
  delayMinutes: number;
  origin: string;
  destination: string;
  timestamp: string;
}

/**
 * Interface for API response structure
 */
export interface ApiResponse<T> {
  timestamp: string;
  data: T[];
}

/**
 * Service for making API calls to the backend
 */
class ApiService {
  /**
   * Fetch flight density data from the API
   * @returns Promise with flight density data
   */
  async getFlightDensity(): Promise<ApiResponse<FlightDensity>> {
    try {
      const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.REST.FLIGHTS_DENSITY}`);
      
      if (!response.ok) {
        throw new Error(`API error: ${response.status} ${response.statusText}`);
      }
      
      return await response.json();
    } catch (error) {
      console.error('Error fetching flight density data:', error);
      throw error;
    }
  }
  
  /**
   * Fetch delayed flights data from the API
   * @returns Promise with delayed flights data
   */
  async getDelayedFlights(): Promise<ApiResponse<DelayedFlight>> {
    try {
      const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.REST.FLIGHTS_DELAYED}`);
      
      if (!response.ok) {
        throw new Error(`API error: ${response.status} ${response.statusText}`);
      }
      
      return await response.json();
    } catch (error) {
      console.error('Error fetching delayed flights data:', error);
      throw error;
    }
  }
}

// Export a singleton instance
export const apiService = new ApiService();
