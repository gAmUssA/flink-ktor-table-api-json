-- Create necessary tables for the Flight Control Center Demo Application

-- Table for current flight positions
CREATE TABLE IF NOT EXISTS current_flight_positions (
    flight_id VARCHAR(50) PRIMARY KEY,
    airline VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    delay_minutes INTEGER NOT NULL DEFAULT 0,
    origin VARCHAR(10) NOT NULL,
    destination VARCHAR(10) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Table for delayed flights
CREATE TABLE IF NOT EXISTS delayed_flights (
    flight_id VARCHAR(50) PRIMARY KEY,
    airline VARCHAR(100) NOT NULL,
    delay_minutes INTEGER NOT NULL,
    origin VARCHAR(10) NOT NULL,
    destination VARCHAR(10) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    notification_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Table for flight density by grid
CREATE TABLE IF NOT EXISTS flight_density (
    grid_lat INTEGER NOT NULL,
    grid_lon INTEGER NOT NULL,
    flight_count BIGINT NOT NULL,
    window_start TIMESTAMP NOT NULL,
    PRIMARY KEY (grid_lat, grid_lon)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_delayed_flights_timestamp ON delayed_flights (timestamp);
CREATE INDEX IF NOT EXISTS idx_flight_density_window ON flight_density (window_start);

-- Create a function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create a trigger to automatically update the updated_at column
CREATE TRIGGER update_current_flight_positions_updated_at
BEFORE UPDATE ON current_flight_positions
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Create a view for active flights (those updated in the last 5 minutes)
CREATE OR REPLACE VIEW active_flights AS
SELECT *
FROM current_flight_positions
WHERE updated_at > (CURRENT_TIMESTAMP - INTERVAL '5 minutes');
