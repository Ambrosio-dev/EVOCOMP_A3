/*
 * Program:   tiny_gp.java
 *
 * Author:    Riccardo Poli (email: rpoli@essex.ac.uk)
 *
 */

import java.util.*;
import java.io.*;
import java.text.DecimalFormat;

public class tiny_gp {
  String fname = "problem.txt";
  double [] fitness;
  char [][] pop;
  static Random rd = new Random();
  static final int
    AND = 110,
    OR = 111,
    NOT = 112,
    FSET_START = AND,
    FSET_END = NOT;
  static boolean [] x = new boolean[FSET_START];
  static char [] program;
  static int PC;
  static int varnumber, fitnesscases, randomnumber;
  // for reference: varnumber is number of variables
  // fitness cases is how many combinations, for boolean expressions it's 2 ^ varnumber
  // randomnumber is the number of terminals (i.e., true, false) that are not variables.
  static double fbestpop = 0.0, favgpop = 0.0;
  static long seed;
  static double avg_len;
  static final int
    MAX_LEN = 10000,
    POPSIZE = 100000,
    DEPTH   = 5,
    GENERATIONS = 100,
    TSIZE = 2;
  public static final double
    PMUT_PER_NODE  = 0.05,
    CROSSOVER_PROB = 0.9;
  static boolean [][] targets;

  boolean run() { /* Interpreter */
    char primitive = program[PC++];
    if ( primitive < FSET_START )
      return(x[primitive]);
    switch ( primitive ) {
      case AND : return( run() && run() );
      case OR : return( run() || run() );
      case NOT : return( run() && !(run()) ); // instead of x1 ! x2, becomes x1 && !x2
      }
    return( false ); // should never get here
  }

  int traverse( char [] buffer, int buffercount ) {
    if ( buffer[buffercount] < FSET_START )
      return( ++buffercount );

    switch(buffer[buffercount]) {
      case AND:
      case OR:
      case NOT:
      return( traverse( buffer, traverse( buffer, ++buffercount ) ) );
      }
    return( 0 ); // should never get here
  }

  void setup_fitness(String fname) {
    try {
      int i,j;
      String line;

      BufferedReader in =
      new BufferedReader(
      		    new
      		    FileReader(fname));
      line = in.readLine();
      StringTokenizer tokens = new StringTokenizer(line);
      varnumber = Integer.parseInt(tokens.nextToken().trim());
      randomnumber = Integer.parseInt(tokens.nextToken().trim());
      fitnesscases = Integer.parseInt(tokens.nextToken().trim());
      targets = new boolean[fitnesscases][varnumber+1];
      if (varnumber + randomnumber >= FSET_START )
        System.out.println("too many variables and constants");

      for (i = 0; i < fitnesscases; i ++ ) {
        line = in.readLine();
        tokens = new StringTokenizer(line);
        for (j = 0; j <= varnumber; j++) {
          targets[i][j] = Boolean.parseBoolean(tokens.nextToken().trim());
      	}
      }
      in.close();
    }
   catch(FileNotFoundException e) {
      System.out.println("ERROR: Please provide a data file");
      System.exit(0);
    }
    catch(Exception e ) {
      System.out.println("ERROR: Incorrect data format " + e);
      System.exit(0);
    }
  }

  double fitness_function( char [] Prog ) {
    int i = 0, len;
    double fit = 1.0;
    boolean result;
    
    len = traverse( Prog, 0 );
    for (i = 0; i < fitnesscases; i ++ ) {
      for (int j = 0; j < varnumber; j ++ )
          x[j] = targets[i][j];
      program = Prog;
      PC = 0;
      result = run();
      if (result == targets[i][varnumber])
      {
    	  fit *= 2; // the fitness becomes 2 ^ number of correct program states
      }
      } 
    
    return( fit );
  }

  int grow( char [] buffer, int pos, int max, int depth ) {
    char prim = (char) rd.nextInt(2);
    int one_child;

    if ( pos >= max )
      return( -1 );

    if ( pos == 0 )
      prim = 1;

    if ( prim == 0 || depth == 0 ) {
      prim = (char) rd.nextInt(varnumber + randomnumber);
      buffer[pos] = prim;
      return(pos+1);
      }
    else  {
      prim = (char) (rd.nextInt(FSET_END - FSET_START + 1) + FSET_START);
      switch(prim) {
      case AND:
      case OR:
      case NOT:
        buffer[pos] = prim;
	one_child = grow( buffer, pos+1, max,depth-1);
	if ( one_child < 0 )
		return( -1 );
        return( grow( buffer, one_child, max,depth-1 ) );
      }
    }
    return( 0 ); // should never get here
  }

  int print_indiv( char []buffer, int buffercounter ) {
    int a1=0, a2;
    if ( buffer[buffercounter] < FSET_START ) {
      if ( buffer[buffercounter] < varnumber )
        System.out.print( "X"+ (buffer[buffercounter] + 1 )+ " ");
      else
        System.out.print( x[buffer[buffercounter]]);
      return( ++buffercounter );
      }
    switch(buffer[buffercounter]) {
      case AND: System.out.print( "(");
        a1=print_indiv( buffer, ++buffercounter );
        System.out.print( " && ");
        break;
      case OR: System.out.print( "(");
        a1=print_indiv( buffer, ++buffercounter );
        System.out.print( " || ");
        break;
      case NOT: System.out.print( "(");
        a1=print_indiv( buffer, ++buffercounter );
        System.out.print( " && !");
        break;
      }
    a2=print_indiv( buffer, a1 );
    System.out.print( ")");
    return( a2);
  }


  static char [] buffer = new char[MAX_LEN];
  char [] create_random_indiv( int depth ) {
    char [] ind;
    int len;

    len = grow( buffer, 0, MAX_LEN, depth );

    while (len < 0 )
      len = grow( buffer, 0, MAX_LEN, depth );

    ind = new char[len];

    System.arraycopy(buffer, 0, ind, 0, len );
    return( ind );
  }

  char [][] create_random_pop(int n, int depth, double [] fitness ) {
    char [][]pop = new char[n][];
    int i;

    for ( i = 0; i < n; i ++ ) {
      pop[i] = create_random_indiv( depth );
      fitness[i] = fitness_function( pop[i] );
      }
    return( pop );
  }


  void stats( double [] fitness, char [][] pop, int gen ) {
    int i, best = rd.nextInt(POPSIZE);
    int node_count = 0;
    fbestpop = fitness[best];
    favgpop = 0.0;

    for ( i = 0; i < POPSIZE; i ++ ) {
      node_count +=  traverse( pop[i], 0 );
      favgpop += fitness[i];
      if ( fitness[i] > fbestpop ) {
      best = i;
      fbestpop = fitness[i];
      }
    }
    avg_len = (double) node_count / POPSIZE;
    favgpop /= POPSIZE;
    System.out.print("Generation="+gen+" Avg Fitness="+(favgpop)+
    		 " Best Fitness="+(fbestpop)+" Avg Size="+avg_len+
    		 "\nBest Individual: ");
    print_indiv( pop[best], 0 );
    System.out.print( "\n");
    System.out.flush();
  }

  int tournament( double [] fitness, int tsize ) {
    int best = rd.nextInt(POPSIZE), i, competitor;
    double  fbest = -1.0e34;

    for ( i = 0; i < tsize; i ++ ) {
      competitor = rd.nextInt(POPSIZE);
      if ( fitness[competitor] > fbest ) {
        fbest = fitness[competitor];
        best = competitor;
      }
    }
    return( best );
  }

  int negative_tournament( double [] fitness, int tsize ) {
    int worst = rd.nextInt(POPSIZE), i, competitor;
    double fworst = 1e34;

    for ( i = 0; i < tsize; i ++ ) {
      competitor = rd.nextInt(POPSIZE);
      if ( fitness[competitor] < fworst ) {
    	fworst = fitness[competitor];
    	worst = competitor;
        }
    }
    return( worst );
  }

  char [] crossover( char []parent1, char [] parent2 ) {
    int xo1start, xo1end, xo2start, xo2end;
    char [] offspring;
    int len1 = traverse( parent1, 0 );
    int len2 = traverse( parent2, 0 );
    int lenoff;

    xo1start =  rd.nextInt(len1);
    xo1end = traverse( parent1, xo1start );

    xo2start =  rd.nextInt(len2);
    xo2end = traverse( parent2, xo2start );

    lenoff = xo1start + (xo2end - xo2start) + (len1-xo1end);

    offspring = new char[lenoff];

    System.arraycopy( parent1, 0, offspring, 0, xo1start );
    System.arraycopy( parent2, xo2start, offspring, xo1start,
    		  (xo2end - xo2start) );
    System.arraycopy( parent1, xo1end, offspring,
    		  xo1start + (xo2end - xo2start),
    		  (len1-xo1end) );

    return( offspring );
  }

  char [] mutation( char [] parent, double pmut ) {
    int len = traverse( parent, 0 ), i;
    int mutsite;
    char [] parentcopy = new char [len];

    System.arraycopy( parent, 0, parentcopy, 0, len );
    for (i = 0; i < len; i ++ ) {
      if ( rd.nextDouble() < pmut ) {
      mutsite =  i;
      if ( parentcopy[mutsite] < FSET_START )
        parentcopy[mutsite] = (char) rd.nextInt(varnumber+randomnumber);
      else
        switch(parentcopy[mutsite]) {
      	case AND:
      	case OR:
      	case NOT:
           parentcopy[mutsite] =
              (char) (rd.nextInt(FSET_END - FSET_START + 1)
                     + FSET_START);
        }
      }
    }
    return( parentcopy );
  }

  static void print_parms() {
   System.out.print("-- TINY GP (Java version) --\n");
   System.out.print("SEED="+seed+"\nMAX_LEN="+MAX_LEN+
   	    "\nPOPSIZE="+POPSIZE+"\nDEPTH="+DEPTH+
     	    "\nCROSSOVER_PROB="+CROSSOVER_PROB+
     	    "\nPMUT_PER_NODE="+PMUT_PER_NODE+
     	    "\nGENERATIONS="+GENERATIONS+
     	    "\nTSIZE="+TSIZE+
     	    "\n----------------------------------\n");
  }

  static void print_gen(int genNum) {
    System.out.print("\n----------------------------------\n"
    +"ATTEMPT "+genNum+": "+
            "\n----------------------------------\n");
   }

  public tiny_gp( String fname, long s ) {
    fitness =  new double[POPSIZE];
    seed = s;
    if ( seed >= 0 )
        rd.setSeed(seed);
    setup_fitness(fname);
    for ( int i = 0; i < FSET_START; i ++ )
      x[i]= rd.nextBoolean();
    pop = create_random_pop(POPSIZE, DEPTH, fitness );
  }

  void evolve() {
    int gen = 0, indivs, offspring, parent1, parent2, parent;
    double newfit;
    char []newind;
    //print_parms();
    stats( fitness, pop, 0 );
    for ( gen = 1; gen < GENERATIONS; gen ++ ) {
      if (  fbestpop >= Math.pow(2, fitnesscases) ) { // best has fitness 2 ^ fitnesscases
      System.out.print("PROBLEM SOLVED\n");
      return;
      }
      for ( indivs = 0; indivs < POPSIZE; indivs ++ ) {
      if ( rd.nextDouble() < CROSSOVER_PROB  ) {
        parent1 = tournament( fitness, TSIZE );
        parent2 = tournament( fitness, TSIZE );
        newind = crossover( pop[parent1],pop[parent2] );
      }
      else {
        parent = tournament( fitness, TSIZE );
        newind = mutation( pop[parent], PMUT_PER_NODE );
      }
      newfit = fitness_function( newind );
      offspring = negative_tournament( fitness, TSIZE );
      pop[offspring] = newind;
      fitness[offspring] = newfit;
      }
      stats( fitness, pop, gen );
    }
    System.out.print("PROBLEM *NOT* SOLVED\n");
    System.exit( 1 );
  }

  public static void main(String[] args) {
    String fname = "problem.txt";
    long[] s = new long[5];
    long one = 1;
    for ( int i = 1; i < 5; i ++ ) {
      s[i] = one*i; 
    }
    int genNum = 1;
    /*
    if ( args.length == 2 ) {
      s = Integer.valueOf(args[0]).intValue();
      fname = args[1];
    } */
    if ( args.length == 1 ) {
      fname = args[0];
    }

    print_parms();

    print_gen(genNum);
    genNum += 1;
    tiny_gp gp1 = new tiny_gp(fname, s[0]);
    gp1.evolve();
    
    print_gen(genNum);
    genNum += 1;
    tiny_gp gp2 = new tiny_gp(fname, s[1]);
    gp2.evolve();

    print_gen(genNum);
    genNum += 1;
    tiny_gp gp3 = new tiny_gp(fname, s[2]);
    gp3.evolve();

    print_gen(genNum);
    genNum += 1;
    tiny_gp gp4 = new tiny_gp(fname, s[3]);
    gp4.evolve();

    print_gen(genNum);
    tiny_gp gp5 = new tiny_gp(fname, s[4]);
    gp5.evolve();
  }
};