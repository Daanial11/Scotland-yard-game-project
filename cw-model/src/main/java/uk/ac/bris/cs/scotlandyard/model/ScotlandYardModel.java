package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

import java.io.StreamCorruptedException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.graph.*;

import uk.ac.bris.cs.gamekit.graph.Graph;

import javax.security.auth.callback.Callback;


// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame {
	List<Boolean> rounds;
	ImmutableGraph<Integer, Transport> graph;
	ArrayList<Colour> playerColour;
	ArrayList<ScotlandYardPlayer> players;
	int playerOrder = 0;
	int roundNumber = 0;
	Set<Colour> winners = new HashSet<>();

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {

		requireNonNull(rounds);
		requireNonNull(graph);
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
		for (PlayerConfiguration configuration : restOfTheDetectives){
			configurations.add(requireNonNull(configuration));
		}
		configurations.add(0, firstDetective);
		configurations.add(0, mrX);



		// Checks if there are any duplicate colours or locations
		Set<Integer> set = new HashSet<>();
		Set<Colour> setColour = new HashSet<>();
		for (PlayerConfiguration configuration : configurations) {
			if (set.contains(configuration.location)) {
				throw new IllegalArgumentException("Duplicate location");
			}
			if (setColour.contains(configuration.colour)) {
				throw new IllegalArgumentException("Duplicate colour");
			}
			set.add(configuration.location);
			setColour.add(configuration.colour);
		}
		//Checks if detectives have any secret or double tickets and if all ticket types are present even if they are 0.
		//Note tickets has the type Map<key, value>, using the 'get' method of Map with a given key e.g. double or
		//secret we can retrieve the value for the given key and compare it with 0. So no need for an array list!

		for (PlayerConfiguration configuration : configurations) {
			if (configuration.tickets.keySet().size() != 5){
				throw new IllegalArgumentException("Missing ticket type");
			}
			if (configuration.colour != BLACK) {
				if (configuration.tickets.get(DOUBLE) !=0) {
					throw new IllegalArgumentException("Detective shouldn't have double ticket");
				}
				if (configuration.tickets.get(SECRET) !=0) {
					throw new IllegalArgumentException("Detective shouldn't have secret ticket");
				}
			}
		}
		this.graph = new ImmutableGraph<>(graph);
		this.rounds = rounds;
		this.players = new ArrayList<>();
		for (PlayerConfiguration configuration : configurations) {
			players.add(new ScotlandYardPlayer(configuration.player, configuration.colour, configuration.location, configuration.tickets));
		}
		this.playerColour = new ArrayList<>();
		for (ScotlandYardPlayer player : players) {
			playerColour.add(player.colour());
		}
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
	public interface callback{

	}

	Set<Move> Moves = new HashSet<>();
	Consumer<Move> moveConsumer;

	@Override
	public void startRotate() {
		playerOrder = 0;
		Integer playerLocation = getPlayerLocation(getCurrentPlayer()).get();
		
		player(getCurrentPlayer()).makeMove(ScotlandYardModel.this, playerLocation, Moves, moveConsumer);
		//movesMade.accept(new PassMove(getCurrentPlayer()));
		if (roundNumber > rounds.size()) {
			isGameOver();
		}
		roundNumber++;
		// TODO
	}
	private Player player(Colour colour){
		Player playerID = players.get(1).player();
		for(ScotlandYardPlayer person: players){
			if(person.colour() == colour){
				return person.player();
			}
		}

		return playerID;
	}

	private Set<Move> validMove(Colour player){
		Set<Move> Moves = new HashSet<Move>();
		//Moves.addAll(getGraph().getEdgesFrom(getPlayerLocation(player)))
		return Moves;
	}


	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Colour> getPlayers() {
        return Collections.unmodifiableList(playerColour);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		if(isGameOver() == true)
		return Collections.unmodifiableSet(winners);
		// TODOd
		return Collections.unmodifiableSet(winners);
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		Optional<Integer> playerLocation = Optional.empty();
		for (ScotlandYardPlayer person : players){
			if (person.colour() == colour){
				playerLocation = Optional.of(person.location());
				if ((rounds.get(roundNumber) == false) && colour.equals(BLACK))
					playerLocation = Optional.of(0);
				return playerLocation;
			}
		}
		return playerLocation;
		// TODO
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		//go through players, if colour given player equals this.colour return player.ticket(ticket) (which is int).
		Optional<Integer> ticketCount = Optional.empty();
		for(ScotlandYardPlayer person : players){
			if(person.colour() == colour){
				ticketCount = Optional.of(person.tickets().get(ticket));
			}
		}
		return ticketCount;
	}

	@Override
	public boolean isGameOver() {
		int posX =0;
		for (ScotlandYardPlayer person : players) {
			if (person.colour() == BLACK)
				posX = person.location();
			if (person.location() == posX && person.isDetective()) {
				winners.addAll(playerColour);
				winners.remove(BLACK);
				return true;
			}
		}
		if (roundNumber > rounds.size()) {
			winners.add(BLACK);
			return true;
		}
		return false;
		// TODO
	}
	Colour currentPlayer;
	@Override
	public Colour getCurrentPlayer() {
		currentPlayer = playerColour.get(playerOrder);
		playerOrder++;
		return currentPlayer;
		// TODO
	}

	@Override
	public int getCurrentRound() {
		return roundNumber;
		// TODO
	}

	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(rounds);
		// TODO
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		return graph;
		// TODO
	}

}
