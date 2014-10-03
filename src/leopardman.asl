// Agent leopardman in project iDM.mas2j

/* Initial beliefs and rules */

hungry :- life(X) & X < 50.

/* Initial goals */

!roam.

/* Plans */

// ****************************************************************************
// 									STAY ALIVE
// ****************************************************************************

// Plan l1
// make the agent die if life is null
@l1
+life(0)
	<-	.print("AAAARGGHHH!!!");
		.my_name(Z);
		.kill_agent(Z).

// Plan l2
// use life() as a trigger because constantly changing instead of food() 
// which is never changing in BB
@l2
+life(_) :	hungry & 					// search for food if I'm hungry
			food(X,Y) & 				// if I know where is some food
			not searchingfood & 		// if I'm not already ongoing
			not chasing(_,_,_)			// and I'm not chasing 
										// i.e. agent prefer to chase preys
	<-	.print("searching for food");
		.drop_all_intentions;			// then stop ongoing activites right now
		?at(X1,Y1);
		if (X1 \== X | Y1 \== Y)
		{
			find_path(X,Y)					// and plan a path to food
		};
		+searchingfood;
		!at(X,Y).						// and run to it
		
// ****************************************************************************
// 									CHASE
// ****************************************************************************

// Plan c1
// if hungry and not already chasing and hidden
// select the closest prey
// chase it
@c1
+other(_,_,_,_) :	hungry 	&				// chase iff I'm hungry
					not chasing(_,_,_) & 	// chase once at a time
					not spying(_)			// chase if I'm not spying 
											// i.e. I can't be seen by humans
	<-	// select a prey 
		.findall(prey(D1,Z1,X1,Y1), other(Z1,X1,Y1,D1) & .substring("rabbit",Z1), L );
		// we might have no preys in the list, check that
		.length(L, N);
		if (N > 0)
		{
			// checked list is not empty; then sort and extract closest prey
			.sort(L,L2);
			.nth(0, L2, prey(D1,Z1,X1,Y1));
			//.print("chasing ", Z1, " at (",X1,",",Y1,")");
			.abolish(searchingfood);
			.drop_all_intentions;
			+chasing(Z1,X1,Y1)	// chase prey Z
		}.

// Plan c2
// chase a known prey Z not reached yet
// check Z is stills the best prey
// check Z has not moved
// else chase again
@c2
+other(Z,_,_,_) : 	chasing(Z,X,Y) & 
					not at(X,Y)
	<-	// check preys 
		.findall(prey(D1,Z1,X1,Y1), other(Z1,X1,Y1,D1) & .substring("rabbit",Z1), L );
		// sort and extract closest prey
		.length(L, N);
		if (N > 0)
		{
			.sort(L,L2);
			.nth(0, L2, prey(D,Z1,X1,Y1));
			// get very last position of the current prey Z
			//.print("currently chasing ", Z1, " at (",X,",",Y,")");
			?chasing(Z,X,Y);
			// now, check the closest prey is the one I am chasing or has moved before to replan
			if (Z \== Z1 | X \== X1 | Y \== Y1)
			{
				//.print("retargeting ", Z1, " to (",X1,",",Y1,")");
				.drop_all_intentions;
				-+chasing(Z1,X1,Y1)		// new chase
			}
		}.

// Plan c3
// check we actually lost a chased prey
// the event has been removed but that not says we still does not perceive the prey
// (only position and distance could have changed)... then we must check we do not
// have new events about the prey before to conclude we really lost it...
@c3
-other(Z,_,_,_) :	chasing(Z,X,Y) & 
					not at(X,Y)
	<-
		.findall(Z, other(Z,_,_,_), L );
		.length(L, N);
		if (N == 0)
		{
			// we lost the prey Z, drop intentions to capture it
			//.print("lost prey ",Z);
			.drop_all_intentions;
			-chasing(Z,X,Y);
			!roam
		}.
		
// Plan c4
// Chase a prey Z at coordinates (X,Y) when we've not reached it yet. 
// Search for a path to reach it. 
@c4
+chasing(Z,X,Y) : not at(X,Y)				
	<-	.print("chasing ", Z, " at (",X,",",Y,")");
		find_path(X,Y);					// and plan a path to target
		!at(X,Y).						// and run to it

// Plan c5
// Chase a prey Z at coordinates (X,Y). We've reached it yet so, do not search 
// for a path and run to it instead...
@c5
+chasing(Z,X,Y) : at(X,Y)	<-	!at(X,Y).

// ****************************************************************************
// 									ROAM
// ****************************************************************************

// Plan r1		
// roaming behaviour
@r1
+!roam : not goingto(X,Y)
	<-	//.print("searching for a new destination...");
		find_path(-1,-1);
		!!roam.

// Plan r2
// roaming behaviour
@r2
+!roam : goingto(X,Y)
	<-	//.print("roaming to location (", X, ",", Y, ")");
		!at(X,Y).

// ****************************************************************************
// 									SPY
// ****************************************************************************

// Plan s1
// we perceive a human, trigger a spying behaviour.
@s1
+human(Z,X,Y,D) :	not searchingfood &		// get food more important than spy
					not chasing(_,_,_) & 	// chase food, idem
					not spying(Z)			// spy once at a time
	<-	.print("spying ", Z);
		.drop_all_intentions;
		+spying(Z);		// mental note I'm syping Z
		!spy(Z).

// Plan s2
// we might have lost the event of what we spy (Z) but we might have a new event
// for it as well. Check we actually lost Z.
// If we were doing nothing important, restart a roaming behaviour
@s2
-human(Z,_,_,_) :	spying(Z) & 			// can lose it iff was spying it
					not searchingfood & 	// get food more important than roam
					not chasing(_,_,_) 		// chase food, idem
	<-	.findall(Z, human(Z,_,_,_), L);
		.length(L, N);
		if (N == 0)
		{
			// no new event for Z, we really lost it
			.print("lost ", Z);
			-spying(Z);
			// change behaviour back to roaming
			.abolish(goingto(_,_));
			.drop_all_intentions;
			!roam
		}.
		
// Plan s3
// we might have lost the event of what we spy (Z) but we might have a new event
// for it as well. Check we actually lost Z.
// Something else important ongoing? continue...
@s3
-human(Z,_,_,_) : spying(Z)
	<-	.findall(Z, human(Z,_,_,_), L);
		.length(L, N);
		if (N == 0)
		{
			// no new event for Z, we really lost it
			.print("lost human ", Z);
			-spying(Z)
		}.
		
// Plan s4
// Spied target in range, look at it
@s4
+!spy(Z) : human(Z,X,Y,D) & D >= 5 & D <= 6
	<-	//.print("spying ", Z);
		watch(X,Y);
		.wait(100);
		!!spy(Z).
		
// Plan s5
// Spied target too close, flee it.
@s5
+!spy(Z) : human(Z,X,Y,D) & D < 5
	<-	//.print("fleeing ", Z);
		flee(X,Y);
		.wait(100);
		!!spy(Z).

// Plan s6
// Spied target too far, follow it.
@s6
+!spy(Z) : human(Z,X,Y,D) & D > 6
	<-	//.print("following ", Z);
		follow(X,Y);
		.wait(100);
		!!spy(Z).

// Plan s7
// spy() failure ; let s2 and s3 handle this.
@s7
-!spy(Z) : true <- true.

// ****************************************************************************
// 									MOVE
// ****************************************************************************

// Plan a1
// not reached any destination, keep ongoing
@a1
+!at(X,Y) : not at(X,Y)
	<-	//?at(X1,Y1);
		//.print("at (", X1, ",", Y1, ") and ongoing to (",X, ",", Y, ")");
		//.print("ongoing to (",X, ",", Y, ")");
		move_on_path;
		.wait(100);
		!!at(X,Y).

// Plan a2
// reached roaming destination - restart it
@a2
+!at(X,Y) :	at(X,Y) & 				// we've arrived at(X,Y)
			not searchingfood & 	// and we were not seraching for food
			not chasing(_,_,_) 		// and we were not chasing
	<-	//.print("arrived to destination.");
		.wait(1000);
		!roam.

// ****************************************************************************
// 									EAT FOOD
// ****************************************************************************

// Plan a3
// reached food, eat it
@a3
+!at(X,Y) : at(X,Y)	& 		// we've reached destination
			food(X,Y) & 	// food is here
			searchingfood	// we were searching for food
	<-	.print("eats food");
		eat(food);
		-searchingfood;
		.wait(1000);
		!roam.
		
// ****************************************************************************
// 							ATTACK, KILL AND EAT
// ****************************************************************************

// Plan a4
// arrived to prey while chasing; attack (might fail), kill and eat
@a4
+!at(X,Y) : at(X,Y) & 		// we've reached destination
			chasing(Z,X,Y)	// we were chasing
	<-	//?at(X1,Y1);
		//.print("at (", X1, ",", Y1, ") and ongoing to (",X, ",", Y, ")");
		.print("attacks ", Z);
		attack(Z);		// might fail
		.print("killed ", Z);
		.kill_agent(Z);
		.print("eats ", Z);
		eat(Z);
		-chasing(Z,X,Y);
		.wait(1000);
		!roam.

// Plan a5
// Attack or chase failure plan
@a5
-!at(X,Y) : chasing(Z,X,Y)
	<-	.print("missed or failed to catch ", Z);
		-chasing(Z,X,Y);
		!roam.

