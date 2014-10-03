// Agent mandog in project iDM.mas2j

/* Initial beliefs and rules */

hungry :- life(X) & X<60.

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

// Plan r1
// Roaming behaviour : search path to destination
@r1
+!roam :	not goingto(_,_) &
			not hungry
	<-	//.print("is searching for a path...");
		find_path(-1,-1);
		!!roam.

// Plan r2
// Roaming behaviour : setup destination when searching for food
@r2
+!roam :	not goingto(_,_) & 
			hungry & 
			food(X,Y)
	<-	//.print("is searching for food...");
		find_path(X,Y);
		!!roam.

// Plan r3
// Roaming behaviour : setup ongoing to destination
@r3
+!roam : goingto(X,Y)
	<-	//.print("is ongoing to location (", X, ",", Y, ")");
		!at(X,Y).

// Plan a1
// Move when ongoing to destination
@a1
+!at(X,Y) : not at(X,Y)
	<-	move_on_path;
		.wait(250);
		!!at(X,Y).

// Plan a2
// Move and arrived when roaming
@a2
+!at(X,Y) :	at(X,Y) & 
			not food(X,Y)
	<-	//.print("is arrived...");
		.wait(1000);
		!roam.

// Plan a3
// Move and arrived when seraching for food
@a3
+!at(X,Y) :	at(X,Y)	& 
			food(X,Y)
	<-	//.print("is arrived and eats food,");
		eat(food);
		.wait(5000);
		!roam.

