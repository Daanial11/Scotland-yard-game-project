package uk.ac.bris.cs.scotlandyard.model;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import uk.ac.bris.cs.gamekit.graph.Graph;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame {

	private final List<Boolean> rounds;
	private final Graph<Integer, Transport> graph;

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
							 PlayerConfiguration mrX, PlayerConfiguration firstDetective,
							 PlayerConfiguration... restOfTheDetectives) {
		this.rounds = requireNonNull(rounds);
		this.graph = requireNonNull(graph);
		if (rounds.isEmpty()) {
			throw new IllegalArgumentException("Empty rounds");
		}
		if (graph.isEmpty()) {
			throw new IllegalArgumentException("Empty graph");
		}
		if (mrX.colour != BLACK) { // or mr.colour.isDetective()
			throw new IllegalArgumentException("MrX should be Black");
		}
		ArrayList<PlayerConfiguration> configurations = new ArrayList<>();
		for (PlayerConfiguration configuration : restOfTheDetectives)
			configurations.add(requireNonNull(configuration));
		configurations.add(0, firstDetective);
		configurations.add(0, mrX);

		// Checks if there are any duplicate colours or locations
		Set<Integer> set = new HashSet<>();
		Set<Colour> setColour = new HashSet<>();
		for (PlayerConfiguration configuration : configurations) {
			if (set.contains(configuration.location))
				throw new IllegalArgumentException("Duplicate location");
			if (setColour.contains(configuration.colour))
				throw new IllegalArgumentException("Duplicate colour");
			set.add(configuration.location);
			setColour.add(configuration.colour);
		}
		// checks if players have the correct tickets
		//Set<Ticket> setTicket = new HashSet<>();


		//USE KEYSET AND VALUES() TO PROPERLY PASS THIS TEST. (CHECK 4TH AND 5TH ELEMENT TO MAKE SURE =0 FOR DECTECTIVES)


		for (PlayerConfiguration configuration : configurations) {
			List ticketList = new ArrayList(configuration.tickets.values());
			if (configuration.colour != BLACK) {
				if (!ticketList.get(4).equals(0) ) {
					throw new IllegalArgumentException("Detective shouldn't have secret ticket");
				}
				if (!ticketList.get(3).equals(0)) {
					throw new IllegalArgumentException("Detective shouldn't have double ticket");
				}


				if (configuration.tickets.keySet().size() != 5){
					throw new IllegalArgumentException("Missing ticket type");
				}
				if (configuration.colour == BLACK){
					if (!configuration.tickets.containsKey(SECRET)) {
						throw new IllegalArgumentException("MRx should have secret ticket");
					}
					if (!configuration.tickets.containsKey(DOUBLE)) {
						throw new IllegalArgumentException("MRx should have double ticket");
					}


					if (configuration.tickets.keySet().size() != 5){
						throw new IllegalArgumentException("Missing ticket type");
					}
				}

			}
		}


		// TODO
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void startRotate() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Colour> getPlayers() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		// TODOd
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public boolean isGameOver() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Colour getCurrentPlayer() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public int getCurrentRound() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Boolean> getRounds() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		// TODO
		throw new RuntimeException("Implement me");
	}

}
