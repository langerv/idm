// Agent hyenapig in project iDM.mas2j

/* Initial beliefs and rules */

hungry :- life(X) & X<50.

/* Initial goals */

!roam.

/* Plans */

// Plan l1
// make the agent die if life is null
@l1
+life(0)
	<-	.print("AAAARGGHHH!!!");
		.my_name(X);
		.kill_agent(X).

// Plan l2
// use life() as a trigger because constantly changing instead of food() 
// which is never changing in BB
@l2
+life(_) :	hungry &
			food(X,Y) &
			not searchingfood
	<-	//.print("searching for food");
		.drop_all_intentions;			// then stop ongoing activites right now
		?at(X1,Y1);
		if (X1 \== X | Y1 \== Y)
		{
			find_path(X,Y)					// and plan a path to food
		};
		+searchingfood;
		!at(X,Y).						// and run to it

// Plan r1
// roaming behaviour: select path
@r1
+!roam : not goingto(_,_)
	<-	//.print("is searching for a new destination...");
		find_path(-1,-1);
		!!roam.

// Plan r2
// roaming behaviour: setup going to destination
@r2
+!roam : goingto(X,Y)
	<-	//.print("is ongoing to location (", X, ",", Y, ")");
		!at(X,Y).

// Plan a1
// Move to destination
@a1
+!at(X,Y) : not at(X,Y)
	<-	move_on_path;
		.wait(200);
		!!at(X,Y).
		
// Plan a2
// Move and arrived when roaming
@a2
+!at(X,Y) :	at(X,Y) &
			not searchingfood
	<-	//.print("is arrived...");
		.wait(1000);
		!roam.

// Plan a3
// Move and arrived when seraching for food
@a3
+!at(X,Y) :	at(X,Y)	& 
			searchingfood
	<-	//.print("is arrived and eats food,");
		-searchingfood;
		eat(food);
		.wait(3000);
		!roam.

