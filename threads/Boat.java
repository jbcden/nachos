package nachos.threads;
import nachos.ag.BoatGrader;
import java.util.ArrayList;

public class Boat
{
  static BoatGrader bg;

  static Island boatLocation = Island.OAHU;

  static ArrayList<KThread> threads = new ArrayList<KThread>();

  static int totalChildren;
  static int totalAdults;

  static int oahuChildren = 0;
  static int oahuAdults = 0;

  static int molokaiChildren = 0;
  static int molokaiAdults = 0;

  // Synchronization mechanisms
  static Lock l = new Lock();
  static Lock l2 = new Lock();

  static Condition boatHere = new Condition(l);
  static Condition boatFull = new Condition(l);
  static Condition boatValid = new Condition(l);

  static Condition waitForPassenger = new Condition(l);
  static Condition waitForRower = new Condition(l);

  static Condition testComplete = new Condition(l);

  /* static Semaphore waitForRower = new Semaphore(0); */

  static Semaphore peopleOnBoat = new Semaphore(1);
  static Semaphore bLocation = new Semaphore(1);

  static Semaphore mChildren = new Semaphore(1);
  static Semaphore mAdults = new Semaphore(1);

  static Semaphore oChildren = new Semaphore(1);
  static Semaphore oAdults = new Semaphore(1);

  static ArrayList<Person> boatPeople = new ArrayList<Person>();

  static enum Island {
    OAHU, MOLOKAI
  }

  static enum Person {
    CHILD, ADULT
  }

  static int getNumberOfChildren(Island current) {
    if(current == Island.MOLOKAI) {
      return molokaiChildren;
    } else {
      return oahuChildren;
    }
  }

  static int getNumberOfAdults(Island current) {
    if(current == Island.MOLOKAI) {
      return molokaiAdults;
    } else {
      return oahuAdults;
    }
  }

  static int getTotalNumberOfPeople(Island current) {
    return getNumberOfChildren(current) + getNumberOfAdults(current);
  }

  static boolean isBoatHere(Island current) {
    return boatLocation == current;
  }

  public static void selfTest()
  {
    BoatGrader b = new BoatGrader();

    // assume >= 2 children
    int adults = 0;
    int children = 2;

    System.out.println("\n ***Testing Boats with " + children + " children***");
    begin(adults, children, b);

    //	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
    //  	begin(1, 2, b);

    //  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
    //  	begin(3, 3, b);
  }

  public static void begin( int adults, int children, BoatGrader b )
  {
    // Store the externally generated autograder in a class
    // variable to be accessible by children.
    bg = b;

    // Instantiate global variables here
    totalChildren = children;
    totalAdults = adults;

    // Create threads here. See section 3.4 of the Nachos for Java
    // Walkthrough linkied from the projects page.

    Runnable a = new Runnable() {
      public void run() {
        AdultItinerary();
      }
    };

    for (int i = 0; i < adults; i++) {
      KThread t = new KThread(a);
      t.setName("adult thread " + i);
      threads.add(t);
      t.fork();
    }

    Runnable c = new Runnable() {
      public void run() {
        ChildItinerary();
      }
    };

    for (int i = 0; i < children; i++) {
      KThread t = new KThread(c);
      t.setName("child thread " + i);
      threads.add(t);
      t.fork();
    }

    l.acquire();
    testComplete.sleep();
    l.release();

    // allowed?
    if(!isDone()) {
      l.acquire();
      testComplete.sleep();
      l.release();
    }

    finishAllThreads();
  }

  private static void finishAllThreads() {
    for(int i=0; i<threads.size(); i++) {
      KThread t = threads.get(i);
      t.finish();
    }
  }

  /* This is where you should put your solutions. Make calls
     to the BoatGrader to show that it is synchronized. For
        example:
        bg.AdultRowToMolokai();
        indicates that an adult has rowed the boat across to Molokai
  */
  static void AdultItinerary() {
    oahuAdults += 1; // an adult has arrived on Oahu

    Person personType = Person.ADULT;
    Island currentIsland = Island.OAHU;
  }
  /* Condition boatHere = new Condition(l); */
  /* Condition boatFull = new Condition(l); */
  /*  */
  /* Semaphore peopleOnBoat = new Semaphore(1); */

  static void ChildItinerary() {
    oahuChildren += 1; // a child has arrived on Oahu
    boolean done = false;

    Person personType = Person.CHILD;
    Island currentIsland = Island.OAHU;

    while(true) { // I don't think we can do this :(
      // if the boat isn't here we can't do anything
      // we need to acquire a condition variable for the boat here
      System.out.println("I AM: "+ KThread.currentThread());
      System.out.println("Check if the boat is here should return false if it is: " + !isBoatHere(currentIsland));
      System.out.println("The boat is at: " + boatLocation);
      System.out.println("I am at: " + currentIsland);
      if(!isBoatHere(currentIsland)) {
        l.acquire();
        System.out.println("BOAT SLEEP");
        boatHere.sleep();
        l.release();
        System.out.println("I AM FREE BITCHES!!");
      } else {
        l.acquire();
        System.out.println("BOAT WAKE");
        boatHere.wake();
        l.release();
      }

      // we need to acquire a semaphore for the boatPeople here
      // we need to sleep on some condition variable here
      if(!isBoatValid(personType)) {
        l.acquire();
        System.out.println("I AM NOT A VALID BOAT");
        boatValid.sleep();
        l.release();
      } else {
        l.acquire();
        boatValid.wake();
        l.release();
      }

      peopleOnBoat.P();
      System.out.println("I AM HERE");
      if(boatPeople.isEmpty()) {

        boatPeople.add(personType);
        /* Island tempIsland = rowToOtherIsland(currentIsland, personType); */
        /* System.out.println(boatPeople); */

        /* l.acquire(); */

        /* l.release(); */
        /* System.out.println("PANDAS!!"); */
        /* System.out.println(currentIsland); */
        /* System.out.println(getNumberOfChildren(currentIsland)); */

        // we check for zero b/c we decrease the number in the row method
        if(getNumberOfChildren(currentIsland) > 1 && currentIsland == Island.OAHU) {
          /* System.out.println("FUCK APPLES"); */

          l.acquire();
          waitForRower.wake();
          l.release();
          System.out.println("WOKE THE ROWER");

          System.out.println("WAIT FOR PASSENGER");
          l.acquire();
          waitForPassenger.sleep();
          l.release();
          System.out.println("A PASSENGER HAS APPEARED!!");

          /* waitForRower.V(); */
          /* l.acquire(); */
          /* boatFull.sleep(); */
          /* #<{(| System.out.println("FUCK  ALL"); |)}># */
          /* l.release(); */
        }
        currentIsland = rowToOtherIsland(currentIsland, personType);

        if(getTotalNumberOfPeople(currentIsland) == 0) {
          done = true;
        }

        /* currentIsland = tempIsland; */
        /* System.out.println("THIS NEEDS TO HAPPEN BEFORE CONTEXT SWITCHING: " + KThread.currentThread() + " :" + tempIsland); */
      } else if(isBoatValid(personType)) {

        /* System.out.println("FUCK MOOSE!!!"); */
        /* l.acquire(); */
        /* waitForRower.P(); */
        /* System.out.println("FUCK ROWERS!!!"); */
        /* l.release(); */

        System.out.println("HELP I AM STUCK! IN AN ELSE-IF CASE :O");
        l.acquire();
        waitForRower.sleep();
        l.release();
        System.out.println("A ROWER HAS APPEARED!!");

        l.acquire();
        waitForPassenger.wake();
        l.release();
        System.out.println("WOKE THE PASSENGER");

        boatPeople.add(personType);
        Island tempIsland = rideToOtherIsland(currentIsland, personType);
        /* System.out.println("THIS NEEDS TO HAPPEN BEFORE CONTEXT SWITCHING: " + currentIsland); */

        if(getTotalNumberOfPeople(currentIsland) == 0) {
          done = true;
        }

        currentIsland = tempIsland;

        /* l.acquire(); */
        /* boatFull.wake(); */
        /* l.release(); */

      } else {
        /* System.out.println("NO GOOD"); */
        continue;
      }
      boatPeople.clear();
      peopleOnBoat.V();

      if(done) {
        l.acquire();
        testComplete.wake();
        l.release();
      }

      done = false;

      // is this allowed??
      /* if(!isDone()) { */
      /* #<{(|   peopleOnBoat.P(); |)}># */
      /*   currentIsland = rowToOtherIsland(currentIsland, personType); */
      /*   peopleOnBoat.V(); */
      /* } */
    }
  }

  static Island rideToOtherIsland(Island current, Person p) {
    if(current == Island.OAHU) {
      if (p == Person.CHILD) {
        childRideToMolokai();
      }
      return Island.MOLOKAI;
    } else { // current island is Molokai
      if (p == Person.CHILD) {
        childRideToOahu();
      }
      return Island.OAHU;
    }
  }

  static Island rowToOtherIsland(Island current, Person p) {
    if(current == Island.OAHU) {
      if (p == Person.CHILD) {
        childRowToMolokai();
      } else { // p is an adult
        adultRowToMolokai();
      }
      return Island.MOLOKAI;
    } else { // current island is Molokai
      if (p == Person.CHILD) {
        childRowToOahu();
      } else { // p is an adult
        adultRowToOahu();
      }
      return Island.OAHU;
    }
  }

  static boolean isDone() { // assume correct for now
    return (molokaiChildren == totalChildren) && (molokaiAdults == totalAdults);
  }

  static void SampleItinerary() {
    // Please note that this isn't a valid solution (you can't fit
    // all of them on the boat). Please also note that you may not
    // have a single thread calculate a solution and then just play
    // it back at the autograder -- you will be caught.
    System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
    bg.AdultRowToMolokai();
    bg.ChildRideToMolokai();
    //bg.AdultRideToMolokai(); // this case is invalid
    bg.ChildRideToMolokai();
  }

  // Tells if we can add another person to the boat
  private static boolean isBoatValid(Person p) {
    if( p == Person.CHILD) {
      return (!boatPeople.contains(Person.ADULT) && boatPeople.size() < 2);
    } else {
      return boatPeople.size() == 0;
    }
  }

  private static void adultRowToMolokai() {
    oAdults.P();
    oahuAdults -= 1;
    oAdults.V();

    mAdults.P();
    molokaiAdults += 1;
    mAdults.V();

    bLocation.P();
    boatLocation = Island.MOLOKAI;
    bLocation.V();

    bg.AdultRowToMolokai();
  }

  private static void childRowToMolokai() {
    oChildren.P();
    oahuChildren -= 1;
    oChildren.V();

    mChildren.P();
    molokaiChildren += 1;
    mChildren.V();

    bLocation.P();
    boatLocation = Island.MOLOKAI;
    bLocation.V();

    /* System.out.println("CHildren on Oahu " + oahuChildren); */
    /* System.out.println("CHildren on Molokai " + molokaiChildren); */
    /* System.out.println("Current Island is " + boatLocation); */

    bg.ChildRowToMolokai();
  }

  private static void childRideToMolokai() {
    oChildren.P();
    oahuChildren -= 1;
    oChildren.V();


    mChildren.P();
    molokaiChildren += 1;
    mChildren.V();

    /* bLocation.P(); */
    /* boatLocation = Island.MOLOKAI; */
    /* bLocation.V(); */

    bg.ChildRideToMolokai();
  }

  // this may be superfluous
  private static void adultRowToOahu() {
    oAdults.P();
    oahuAdults += 1;
    oAdults.V();

    mAdults.P();
    molokaiAdults -= 1;
    mAdults.V();

    bLocation.P();
    boatLocation = Island.OAHU;
    bLocation.V();

    bg.AdultRowToOahu();
  }

  private static void childRowToOahu() {
    oChildren.P();
    oahuChildren += 1;
    oChildren.V();

    mChildren.P();
    molokaiChildren -= 1;
    mChildren.V();

    /* System.out.println("If this is not Molokai we in trouble: " +boatLocation); */

    bLocation.P();
    boatLocation = Island.OAHU;
    bLocation.V();

    bg.ChildRowToOahu();
  }

  // may be superfluous
  private static void childRideToOahu() {
    oChildren.P();
    oahuChildren += 1;
    oChildren.V();

    mChildren.P();
    molokaiChildren -= 1;
    mChildren.V();

    bLocation.P();
    boatLocation = Island.OAHU;
    bLocation.V();

    bg.ChildRideToOahu();
  }
}
