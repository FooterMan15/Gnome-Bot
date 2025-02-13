package dev.gnomebot.app.data.ping;

import java.util.Arrays;

public record PingDestinationBundle(PingDestination[] destinations) implements PingDestination {
	public static final PingDestination[] EMPTY_ARRAY = new PingDestination[0];

	@Override
	public void relayPing(PingData data, Ping ping) {
		for (PingDestination destination : destinations) {
			destination.relayPing(data, ping);
		}
	}

	@Override
	public String toString() {
		return Arrays.toString(destinations);
	}
}
