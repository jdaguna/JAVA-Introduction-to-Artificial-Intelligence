//	J. Joshua Daguna, IV
//
// This class implements tools for evaluating game states for the Zombie
// Dice game.  These tools include a static function for calculating
// the expected utility value of a game state using look-ahead to a 
// fixed depth and a static heuristic evaluation function for estimating
// game payoff values for non-terminal states.  Since these tools are
// all static functions, no objects of this class need to be allocated in
// order to use them.  In general, both heuristic evaluation function
// values and expected utility values should be between plus and minus
// "State.win_payoff".
//
// Zombie Dice is a trademark of Steve Jackson Games.  For more information
// about this game, see "zombiedice.sjgames.com".
//
// David Noelle -- Sat Nov  3 16:37:10 PDT 2012
// Assisted by Angelo Kyrilov -- Mon Dec 9, 2013
//


public class Eval {

    // Non-terminal states at this limit should be evaluated using
    // the given heuristic evaluation function ...
    static public int depth_limit = 2;

    // value -- This public function returns the payoff value of 
    //          terminal states or the expected utility value of 
    //          non-terminal states, backing up heuristic evaluation 
    //          values once the given depth has reached the depth limit.
    static public double value (State s, int depth) {
	// Stop searching once either a terminal state is reached or the
	// depth limit is reached ...
	if ((s.terminal()) || (depth >= depth_limit)) {
	    return (s.payoff());
	}
	// Keep searching ...
	switch (s.current_choice) {
	case roll:
	    return (Eval.value_roll(s, depth));
	case stop:
	    return (Eval.value_stop(s, depth));
	case undecided:
	    return (Eval.value_choose(s, depth));
	default:
	// We should never get here ...
	    return (0.0);
	}
    }

    // value -- This public function returns the payoff value of 
    //          terminal states or the expected utility value of 
    //          non-terminal states, backing up heuristic evaluation 
    //          values once the given depth has reached the depth limit.
    static public double value (State s) {
	return (Eval.value(s, 0));
    }

    // value_rolled_hand -- Compute the expected utility value of this 
    //                      state, given that the hand has just been 
    //                      rolled to the specified dice faces.
    static double value_rolled_hand (State rolled_s, int depth) {
	State s = new State(rolled_s);
	double val = 0.0;                // return value

	// Collect brain and blast dice from the hand ...
	s.collectHand();
	// Check to see if the current player has been shotgunned ...
	if (s.shotgunned()) {
	    // This turn is over, so force the choice to stop ...
	    s.current_choice = Choice.stop;
	    // Calculate the expected utility value of the resulting
	    // state by processing the "stop" action ...
	    val = Eval.value(s, depth);
	} else {
	    // The roll is done, but the turn is not, so set the 
	    // choice to undecided ...
	    s.current_choice = Choice.undecided;
	    // Calculate the expected utility value of the resulting 
	    // state.  Note that this is one of the two places where 
	    // the "depth" is incremented ...
	    val = Eval.value(s, (depth + 1));
	}
	// Deallocate storage ...
	s = null;
	// Return the expected value ...
	return (val);
    }

    // value_roll_hand -- Compute the expected utility value of this 
    //                    state, given that the hand is full.  Note that
    //                    this function assumes that there are three dice
    //                    in a hand (i.e., that the value of "have_size" 
    //                    is three).
    static double value_roll_hand (State s, int depth) {
	double val = 0.0;           // return value
	for (DieFace d1 : DieFace.values()){
		if (d1 != DieFace.invalid){
			for (DieFace d2 : DieFace.values()){
				if (d2 != DieFace.invalid){
					for (DieFace d3 : DieFace.values()){
						if (d3 != DieFace.invalid){
							double p = s.rollProb(d1, d2, d3);
							
							s.roll(d1, d2, d3);
							
							double v = value_rolled_hand(s, depth);
							
							val += (v*p);
						}
					}
				}
			}
		}
	}
	
	
	
	return (val);
    }

    // value_roll -- Compute the expected utility value of this state, 
    //               given that the current player will be immediately 
    //               drawing dice and rolling.
    static double value_roll (State s, int depth) {
	double val = 0.0;           // return value

	if (s.numDiceInHand() == State.hand_size) {
	    // No need to draw more dice, so we need to consider all
	    // possible results of rolling the dice in hand ...
	    val = Eval.value_roll_hand(s, depth);
	} else {
	    // Need to draw a die ...
	    if (s.cupIsEmpty()) {
		// The cup is empty.  According to the official rules,
		// we should reuse collected brain dice at this point ...
		State refilled_state = new State(s);
		refilled_state.reuseBrains();
		val = Eval.value_roll(refilled_state, depth);
		refilled_state = null;
	    } else {
		// Iterate over all possible colors for the next die ...
		for (DieColor c : DieColor.values()) {
		    if (c != DieColor.invalid) {
			double this_draw_prob = s.drawProb(c);
			// Draw die of this color ...
			Die d = s.draw(c);
			if (d != null) {
			    // Recursive call ...
			    double draw_val = Eval.value_roll(s, depth);
			    // Update the expected utility value over all 
			    // colors for this die ...
			    val = val + (draw_val * this_draw_prob);
			    // Replace the drawn die in the cup ...
			    s.replace(d);
			}
		    }
		}
	    }
	}
	// Return expected value ...
	return (val);
    }

    // value_stop -- Compute the expected utility value of this state, 
    //               given that the current player will not continue 
    //               to roll at this point.
    static double value_stop (State stop_s, int depth) {
	State s = new State(stop_s);
	double val = 0.0;                // return value

	// Update scores ...
	s.endTurn();
	// Check for end of game ...
	if (s.terminal()) {
	    val = s.payoff();
	} else {
	    // Move to next player ...
	    s.nextPlayer();
	    // Recursively calculate the expected utility value of the
	    // next player's choice node.  Note that this is one of the 
	    // two places where "depth" is incremented.
	    val = Eval.value(s, (depth + 1));
	}
	// Deallocate storage ...
	s = null;
	// Return value ...
	return (val);
    }

    // value_choose -- Compute the expected utility value of each of two 
    //                 actions:  rolling and stopping.  Return the greater 
    //                 of these two values if the computer is the current 
    //                 player, and return the lesser of these two values 
    //                 if the user is the current player.
    static double value_choose (State s, int depth) {
	double eu_roll;  // expected utility value of rolling
	double eu_stop;  // expected utility value of stoping

	// Always roll if no brains have been collected ...
	if (s.brains_collected == 0) {
	    s.current_choice = Choice.roll;
	    eu_roll = Eval.value(s, depth);
	    // Revert the state ...
	    s.current_choice = Choice.undecided;
	    // Return value of rolling ...
	    return (eu_roll);
	}
	// First, calculate the case of choosing to roll ...
	s.current_choice = Choice.roll;
	eu_roll = Eval.value(s, depth);
	// Now, calculate the case of choosing to stop ...
	s.current_choice = Choice.stop;
	eu_stop = Eval.value(s, depth);
	// Revert the state ...
	s.current_choice = Choice.undecided;
	// Which one is better depends on whose turn it is ...
	if (s.current_player == Turn.computer) {
	    // MAX node -- Looking for high values ...
	    if (eu_roll >= eu_stop) {
		return (eu_roll);
	    } else {
		return (eu_stop);
	    }
	} else {
	    // MIN node -- Looking for low values ...
	    if (eu_roll <= eu_stop) {
		return (eu_roll);
	    } else {
		return (eu_stop);
	    }
	}
    }

	// heuristic -- Compute a heuristic evaluation function value for the
	// specified State object. This function must be
	// calculated quickly, with no look-ahead search, and it
	// should be bounded between plus and minus the value of
	// "State.win_payoff". The heuristic evaluation value
	// is returned.
	static public double heuristic(State s) {
		// Heuristic value to be returned ...
		
		// the score difference drives this heuristic. the closer the comp is to winning a higher positive value is returned
		//the closer the user is to winning will return a lower negative value
		double value = 0.0;
		
		int s_diff = s.comp_brains_eaten-s.user_brains_eaten;
		if(s_diff>0){//positive - more computer brains than user brains
			if(s_diff<=2){
				return value =20;
			}else if(s_diff<=4){
				return value =80;
			}else if(s_diff<=8){
				return value = 90;
			}else if(s_diff==9){
				return value = 95;
			}else if(s_diff>=10){
				return value =100;
			}
		}
		
		if(s_diff==0){//computer brains = user brains
			return value = 0;
		}
		
		if(s_diff<0){//negative - more user brains than computer brains
			if(s_diff==-1){
				return value = -10;
			}else if (s_diff==-2){
				return value = -20;
			}else if (s_diff==-3){
				return value = -30;
			}else if (s_diff==-4){
				return value = -40;
			}else if (s_diff==-5){
				return value = -50;
			}else if (s_diff==-6){
				return value = -60;
			}else if (s_diff==-7){
				return value = -70;
			}else if (s_diff==-8){
				return value = -80;
			}else if (s_diff==-9){
				return value = -90;
			}else if (s_diff<=-10){
				return value = -100;
			}
		}
		
		return (value);
	}

}

