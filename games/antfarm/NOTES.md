
Meher Bhandari
Have you ever wondered how simple organisms, like ants, find the shortest path between their nest and a food source? You might think it's due to a particular communication system between them or an ability to perceive distance, but the answer lies in a much simpler chemical substance—pheromones. What's more, this process is a naturally occurring example of emergence, which is the backbone of complex artificial networks that power many generative AI models that we use today.

How do ants make decisions?
To be more specific, pheromones are chemical trails laid by ants as they depart from their nests to visit a food source and subsequently return to their nest. Fellow ants tend to follow the trail with the greatest quantity of pheromone on it. Given this background, we can imagine a simplified example with only two ants traveling, who we'll call TAL (takes a left) and TAR (takes a right). There is an obstacle in their path positioned such that the right path is shorter than the left, as shown in the figure below.

Screenshot 2025-09-20 at 1.42.11 AM.png
Screenshot 2025-09-20 at 1.42.11 AM.png
Figure 1: The Emergent Mind Chapter 2, Gaurav Suri and James McClelland

Because the right path is shorter, TAR arrives at the food source first, and then determines its return path by detecting the pheromone levels at that point. Because the right path is the only one that's been taken so far, TAR only finds pheromone on the right path and uses that one to return. Because the left path is longer, TAL reaches the food source later on, so it detects double the pheromone on the right path compared to the left. Therefore, TAL will also return using the right path. As more ants leave the nest to access the food source, the same process will occur and cause pheromone levels to compound on the right path, meaning that most of the ants will favor the shorter, more efficient trail. The ants' use of pheromones here resembles the concept of emergence in AI—tangible decisions rooted in simplistic logic lead to more advanced conclusions.

Program Overview
Reflecting this process through code, I created a visual of the ants using pheromone trails to navigate to the food source and back. The ants make decisions with some degree of randomness; paths are chosen using a stochastic function so that not every choice is the path with the highest pheromone level. 

 

The program runs using a series of timesteps, during each of which a new ant leaves the nest and selects a path to the food source. Depending on the difference in length between the paths, the program will calculate the ant's arrival time in terms of timesteps and add it to the queue of ants accordingly. After this, any ants that reach the food source on the current timestep will increase the pheromones on their chosen path and then choose a return path. Finally, any ants returning to the nest on the current timestep will also increase the pheromone on their path. 

 

This process repeats until all ants have returned to the nest, with the overall pheromone levels decaying each step to mimic the natural properties of pheromone. The full code can be found here: https://github.com/meher-bhandari/ant-behavior-simulation


Below is a recording of one such simulation playing out:


Simulation Trials
After creating the initial model, I chose to test the impacts of two main variables on the results: distance and noise. For the distance test, I shifted the placement of the obstacle in increments of 25 pixels, testing six differences in path length total. For each trial, I ran the simulation 10 times, with a constant noise level of 0.25, colony size of 100, and evaporation rate of 0.1. I took the averages of the number of ants taking the left and right paths for each obstacle position, and then plotted these values against the difference between the two path lengths, as seen in the following graph.​

Screenshot 2025-09-20 at 1.56.38 AM.png
For the noise test, I chose 5 randomness values to test: 0, 0.25, 0.5, 0.75, and 1. For each noise value, I ran the simulation 10 times, keeping the other parameters constant with the obstacle positioned at -25, the evaporation rate at 0.1, and the colony size at 100. Similarly to the previous trials, I averaged the ants taking the left path and the ants taking the right path for each noise value before plotting them in the graph below.

Screenshot 2025-09-20 at 1.57.18 AM.png
These results are overall consistent with what is expected from the program. In the distance trial, more ants favor the shorter path as the difference in length between the two paths increases. In the noise trial, the number of ants taking each path grows more even as the randomness level increases.

Future Directions
From here, some future directions for this research could be investigating different types of pheromones as they occur naturally. In particular, some species of ants can deposit both short and long-term pheromones depending on the type of trail they are marking, and the next version of the program could reflect that to more accurately model ant behavior.

import random
import tkinter as tk2
from tkinter import simpledialog
import turtle
import matplotlib.pyplot as plt

# left is short, right is long (2x)

skipToEnd = False

def skipToEndHandler(width=400,height=400):
    global skipToEnd
    skipToEnd = True

def distance(x1,x2,y1,y2):
    return ((x1-x2)**2+(y1-y2)**2)**0.5

# y is the top y-coordinate (-50,75)
def turtleSetup(y):

    screen = turtle.Screen()
    btn = tk2.Button(screen._root, text="Skip to End", command=skipToEndHandler)
    btn.pack(side="bottom")
    
    antShape = (
    (10, 0),     # head tip (right)
    (7, 3),      # top head
    (4, 5),      # upper thorax
    (0, 3),      # upper body
    (-3, 6),     # top leg
    (-5, 3),     # rear top
    (-6, 0),     # rear center
    (-5, -3),    # rear bottom
    (-3, -6),    # bottom leg
    (0, -3),     # lower body
    (4, -5),     # lower thorax
    (7, -3),     # bottom head
    (10, 0)      # close shape
)
    turtle.register_shape("ant", antShape)

    # draw food/target
    d = turtle.Turtle()
    d.speed(6)
    d.shape("ant")
    d.up()
    d.goto(200,-50)
    d.down()
    d.color('red')
    d.circle(50)

    # go to start
    d.up()
    d.goto(-50,y)

    # draw obstacle
    d.color('black')
    d.down()
    d.forward(100)
    d.right(90)
    d.forward(150)
    d.right(90)
    d.forward(100)
    d.right(90)
    d.forward(150)
    d.right(90)

    # draw paths
    d.up()
    d.goto(-200,0)
    d.stamp()
    leftLen = takeLeftPath(d,6,y)
    d.showturtle()
    rightLen = takeRightPath(d,6,y)

    d.hideturtle()

    return leftLen,rightLen

def takeLeftPath(t,sp,y):
    t.hideturtle()
    t.shape("ant")
    t.speed(sp)
    t.up()
    t.goto(-200,0)
    t.showturtle()

    coord = y+50
    t.down()
    t.goto(0,coord)
    t.goto(200,0)

    t.hideturtle()

    return distance(-200,0,0,coord) + distance(0,200,coord,0)

def returnLeftPath(t,sp,y):
    t.hideturtle()
    t.shape("ant")
    t.speed(sp)
    t.up()
    t.goto(200,0)
    t.showturtle()

    coord = y+50
    t.down()
    t.goto(0,coord)
    t.goto(-200,0)

    t.hideturtle()

    return distance(-200,0,0,coord) + distance(0,200,coord,0)

def takeRightPath(t,sp,y):
    t.hideturtle()
    t.shape("ant")
    t.speed(sp)
    t.up()
    t.goto(-200,0)
    t.showturtle()

    coord = y-150-100 if y < 50 else y-150-50
    t.down()
    t.goto(0,coord)
    t.goto(200,0)

    t.hideturtle()

    return distance(-200,0,0,coord) + distance(0,200,coord,0)

def returnRightPath(t,sp,y):
    t.hideturtle()
    t.shape("ant")
    t.speed(sp)
    t.up()
    t.goto(200,0)
    t.showturtle()

    coord = y-150-100 if y < 50 else y-150-50
    t.down()
    t.goto(0,coord)
    t.goto(-200,0)

    t.hideturtle()

    return distance(-200,0,0,coord) + distance(0,200,coord,0)

def decay(init,r):
    return init*(1-r)

def pickPath(leftPath,rightPath,noise):
    if leftPath == 0 and rightPath == 0:           # starter random case
        path = 'left' if random.random() < 0.5 else 'right'
    else:
        pherTotal = leftPath+rightPath 
        probLeft = leftPath/pherTotal
        probLeft = max(0, min(1,probLeft+random.uniform(-noise,noise)))   # normalization btw 0 and 1
        path = 'left' if random.random() < probLeft else 'right'
    return path

def timestepSim(obsCoord,evapRate,noise,colonySize,speed):
    global skipToEnd
    skipToEnd = False
    leftLen,rightLen = turtleSetup(obsCoord)
    leftTime = int((leftLen - 400) // 16.1) + 1
    rightTime = int((rightLen - 400) // 16.1) + 1
    departQueue = {}
    returnQueue = {}
    leftPath = rightPath = 0
    leftCount = rightCount = 0
    leftHistory = []
    rightHistory = []
    pherStrength = 10
    time = 0
    ongoing = True

    while ongoing:

        leftPath = decay(leftPath,evapRate)
        rightPath = decay(rightPath,evapRate)

        if time < colonySize: # send out a new ant
            path = pickPath(leftPath,rightPath,noise)
            if path == 'left': key = time + leftTime
            else: key = time + rightTime
            val = departQueue.get(key, [])
            val.append(path)
            departQueue[key] = val

        if time in departQueue:
            arrivers = departQueue.pop(time)
            for ant in arrivers:
                if ant == 'left':
                    leftCount += 1
                    leftPath += pherStrength
                    if not skipToEnd:
                        l = turtle.Turtle()
                        l.hideturtle()
                        l.width(1+0.02*leftCount)
                        takeLeftPath(l,speed,obsCoord)
                else:
                    rightCount += 1
                    rightPath += pherStrength
                    if not skipToEnd:
                        r = turtle.Turtle()
                        r.hideturtle()
                        r.width(1+0.02*rightCount)
                        takeRightPath(r,speed,obsCoord)

                returnPath = pickPath(leftPath,rightPath,noise)
                if returnPath == 'left': key = time + leftTime
                else: key = time + rightTime
                val = returnQueue.get(key, [])
                val.append(returnPath)
                returnQueue[key] = val

        if time in returnQueue:
            returners = returnQueue.pop(time)
            for ant in returners:
                if ant == 'left':
                    leftCount += 1
                    leftPath += pherStrength
                    if not skipToEnd:
                        l = turtle.Turtle()
                        l.hideturtle()
                        l.width(1+0.03*leftCount)
                        returnLeftPath(l,speed,obsCoord)
                else:
                    rightCount += 1
                    rightPath += pherStrength
                    if not skipToEnd:
                        r = turtle.Turtle()
                        r.hideturtle()
                        r.width(1+0.03*rightCount)
                        returnRightPath(r,speed,obsCoord)

        leftHistory.append(leftCount)
        rightHistory.append(rightCount)
        time += 1
        if time >= colonySize and departQueue == {} and returnQueue == {}:
            ongoing = False

    print(f"{leftCount} ants took the left path, {rightCount} ants took the right path.")
    plotAntPaths(leftHistory, rightHistory, colonySize)
    return (leftCount,rightCount)

def plotAntPaths(leftHistory, rightHistory, colonySize):
    x = list(range(len(leftHistory)))  # 0 to colonySize*2
    y = list(range(len(rightHistory)))
    plt.plot(x, leftHistory, label='Left Path', color='green')
    plt.plot(y, rightHistory, label='Right Path', color='orange')
    plt.xlabel("Number of Ants That Have Traveled")
    plt.ylabel("Cumulative Ants Choosing Path")
    plt.title("Ant Path Preference Over Time")
    plt.legend()
    plt.grid(True)
    plt.show()

# start sim on button click

def defaultButtonClick():
    obsCoord = 0
    evapRate = 0.1
    colonySize = 100
    noise = 0.25
    speed = 8
    root.destroy()
    timestepSim(obsCoord,evapRate,noise,colonySize,speed)

def customButtonClick():
    obsCoord,evapRate,noise,colonySize,speed = getInput()
    root.destroy()
    timestepSim(obsCoord,evapRate,noise,colonySize,speed)

def getInput():
    obsCoord = simpledialog.askinteger("Input", "Enter obstacle position between -50 (bottom of sceen) to 75 (top of screen):")
    colonySize = simpledialog.askinteger("Input", "Enter colony size:")
    evapRate = simpledialog.askfloat("Input", "Enter evaporation rate (0–1):")
    noise = simpledialog.askfloat("Input", "Enter noise level (0–1):")
    speed = simpledialog.askinteger("Input","Enter simulation speed (0-10):")
    return (obsCoord,evapRate,noise,colonySize,speed)

root = tk2.Tk()
button1 = tk2.Button(root, text="Start sim with default parameters", command=defaultButtonClick)
button2 = tk2.Button(root, text="Start sim with custom parameters", command=customButtonClick)
button1.pack()
button2.pack()

root.mainloop()


another simulation:
Practicing Ruby
About Github
Simulating the emergent behavior of ant colonies
Nov 27, 2012 • Gregory Brown

This article is based on a heavily modified Ruby port of Rich Hickey’s Clojure ant simulator. Although I didn’t directly collaborate with Rich on this issue of Practicing Ruby, I learned a lot from his code and it provided me with a great foundation to start from.

Watch as a small ant colony identifies and completely consumes its four nearest food sources:


While this search effort may seem highly organized, it is the result of very simple decisions made by individual ants. On each tick of the simulation, each ant decides its next action based only on its current location and the three adjacent locations ahead of it. But because ants can indirectly communicate via their environment, complex behavior arises in the aggregate.

Emergence and self-organization are popular concepts in programming, but far too many developers start and end their explorations into these ideas with Conway’s Game of Life. In this article, I will help you see these fascinating properties in a new light by demonstrating the role they play in ant colony optimization (ACO) algorithms.

NOTE: There are many ways to simulate ant behavior, some of which can be quite useful for a wide range of search applications. For this article, I have built a fairly naïve simulation that is meant to loosely mimic the kind of ant behavior you can observe in the natural world. This article may be useful as a brief introduction to ACO, but be sure to dig deeper if you are interested in practical applications. My goal is to provide a great example of emergent behavior, NOT a great reference for nature-inspired search algorithms.

Modeling the state of an ant colony
This simulated world consists of many cells: some are food sources, some are part of the colony’s nest, and the rest are an open field that needs to be traversed. Each cell can contain a single ant facing in one of the eight directions you’d find on a compass. As the ants move around the world, they mark the cells they visit with a trail of pheromones that helps them find their way between their nest and nearby food sources. Pheromones accumulate as more ants travel across a given trail, but they also gradually evaporate. The combination of these two properties of pheromones helps ants find efficient paths to nearby food sources.

Subtle changes to any of these rules can yield very different outcomes, and finding an optimal result will necessarily involve some experimentation. Knowing that, it makes sense for the simulator to have a data model that is divorced from its domain logic. Many behavioral changes can be made without altering the underlying data model, and that allows the Ant, Cell, and World constructs to be defined as simple value objects as shown below:

module AntSim 
  class Ant
    def initialize(direction, location)
      self.direction = direction
      self.location  = location
    end

    attr_accessor :food, :direction, :location
  end

  class Cell
    def initialize(food, home_pheremone, food_pheremone)
      self.food           = food 
      self.home_pheremone = home_pheremone
      self.food_pheremone = food_pheremone
    end

    attr_accessor :food, :home_pheremone, :food_pheremone, :ant, :home
  end

  class World
    def initialize(world_size)
      self.size = world_size
      self.data = size.times.map { size.times.map { Cell.new(0,0,0) } }
    end

    def [](location)
      x,y = location

      data[x][y]
    end

    def sample
      data[rand(size)][rand(size)]
    end

    def each
      data.each_with_index do |col,x| 
        col.each_with_index do |cell, y| 
          yield [cell, [x, y]]
        end
      end
    end

    private

    attr_accessor :data, :size
  end
end
These classes are somewhat peculiar in that they are very state-centric and do not encapsulate any interesting domain logic. Although it won’t win us object-oriented style points, designing things this way decouples the state of the simulated world from both the events that happen within it and the optimization algorithms that run against it. These objects represent only the nouns of our system, leaving it up to their collaborators to supply the verbs.

Moving around the world
The ants in this system are surprisingly limited in their behavior. On each and every iteration, their entire decision making process can result in exactly one of the following outcomes:

Ant movement rules

Most of these actions are extremely localized. Turning does not affect any cells, while moving only affects the cell the ant currently occupies and the one immediately in front of it. However, taking or dropping food triggers a pheromone update, affecting every cell the ant has visited since the last time it updated its trails. This can have far-reaching effects on the behavior of the rest of the colony, even though each individual ant can only sense the pheromone levels of its own cell and the three cells directly in front of it. While natural ants must drop pheromone continuously as they walk, artificial ants can improve upon nature by updating entire paths instantaneously.

An object that implements these behaviors needs to know about the structure of the Ant, Cell, and World objects, but it still does not need to know much about the core domain logic of the simulator. What we want is an Actor that understands its world and how to play specific roles within it, but does not attempt to define the broader story arc:

require "set"

module AntSim
  class Actor
    DIR_DELTA   = [[0, -1], [ 1, -1], [ 1, 0], [ 1,  1],
                   [0,  1], [-1,  1], [-1, 0], [-1, -1]]

    def initialize(world, ant)
      self.world   = world
      self.ant     = ant

      self.history = Set.new
    end

    attr_reader :ant

    def turn(amt)
      ant.direction = (ant.direction + amt) % 8

      self
    end

    def move
      history << here

      new_location = neighbor(ant.direction)

      ahead.ant = ant
      here.ant  = nil

      ant.location = new_location

      self
    end

    def drop_food
      here.food += 1
      ant.food   = false

      self
    end

    def take_food
      here.food -= 1
      ant.food   = true

      self
    end

    def mark_food_trail
      history.each do |old_cell|
        old_cell.food_pheremone += 1 unless old_cell.food > 0 
      end

      history.clear

      self
    end

    def mark_home_trail
      history.each do |old_cell|
        old_cell.home_pheremone += 1 unless old_cell.home
      end

      history.clear

      self
    end

    def foraging?
      !ant.food
    end

    def here
      world[ant.location]
    end

    def ahead
      world[neighbor(ant.direction)]
    end

    def ahead_left
      world[neighbor(ant.direction - 1)]
    end

    def ahead_right
      world[neighbor(ant.direction + 1)]
    end
    
    def nearby_places
      [ahead, ahead_left, ahead_right]
    end

    private

    def neighbor(direction)
      x,y = ant.location

      dx, dy = DIR_DELTA[direction % 8]

      [(x + dx) % world.size, (y + dy) % world.size]
    end

    attr_accessor :world, :history
    attr_writer   :ant
  end
end
Of course, now that we have crossed the line from pure data models to an object which actually does something, it is impossible to implement meaningful behavior without making certain assumptions that will affect the capabilities of the rest of the system. The Actor class draws two significant lines in the sand that are easy to overlook on a quick glance:

Storing history data in a Set rather than an Array makes it so that when this object updates pheromone trails, it only takes into account what cells were visited, not how many times they were visited or in what order they were traversed.

The modular arithmetic performed in the neighbor function treats the world as if it were a torus, instead of a plane. This means that the leftmost column and the rightmost column of the map are adjacent to one another, as are the top and bottom rows. This allows ants to easily wrap around the edges of the map, but also establishes connections between cells that you may not intuitively think of as being close to one another. Without a three-dimensional visualization, it is hard to show that the top right corner of the map and the bottom left corner are actually adjacent to one another.

Of course, the purpose of the Actor class is to hide these details from the rest of the system. As long as its collaborators can operate within these constraints, the Actor object can be treated as a magic black box that knows how to make ants move around the world and do interesting things. To see why that is useful, check out the Simulator#iterate function which drives the simulator’s main event loop:

module AntSim
  class Simulator
    # ... other functions ...

    def iterate
      actors.each do |actor|
        optimizer = Optimizer.new(actor.here, actor.nearby_places)
        
        if actor.foraging?
          action = optimizer.seek_food
        else
          action = optimizer.seek_home
        end

        case action
        when :drop_food
          actor.drop_food.mark_food_trail.turn(4)
        when :take_food
          actor.take_food.mark_home_trail.turn(4)
        when :move_forward
          actor.move
        when :turn_left
          actor.turn(-1)
        when :turn_right
          actor.turn(1)
        else
          raise NotImplementedError, action.inspect
        end
      end

      sleep ANT_SLEEP
    end
  end
end
Here we can see that the Simulator acts as a bridge that translates the Optimizer object’s very abstract suggestions into concrete actions for the Actor to carry out. The design of the Actor object gives the Simulator just enough control to make some small adjustments to the process, but not so much that it needs to be bogged down with the details.

Finding food and bringing it home
Now that we know the state of the world and how it can be manipulated, it is time to discuss how to produce the kind of behavior that you saw in the video at the beginning of this article. Perhaps unsurprisingly, the life of the everyday worker ant is actually fairly mundane.

Every ant in this simulation is always either searching for food to bring back to the nest, or trying to return home with the food it found. As soon an ant accomplishes one of these tasks, it immediately transitions to the other, not bothering to take even a moment to bask in fruits of its labor. The following outline describes what the ants in this simulation are “thinking” at any given point in time, assuming that they haven’t managed to become self-aware…

When searching for food:

If the current cell has food in it and it is NOT part of the nest, pick up some food.

Otherwise, check the cell directly in front of me. If it has food in it, is not part of the nest, and it is not occupied by another ant, move there.

If not, rank the three adjacent cells in front of me based on the amount of food they contain, and how intense their food_pheremone levels are. I will usually choose to move or turn towards the cell with highest ranking, but I will randomly deviate from this pattern on occasion so that I can explore some uncharted territory.

When searching for the nest:

If the current cell is part of the nest, drop the food I am carrying.

Otherwise, check the cell directly in front of me. If it is part of the nest, and it is not occupied by another ant, move there.

If not, rank the three adjacent cells in front of me based on whether or not they are part of the nest, and how intense their home_pheremone levels are. I will usually choose to move or turn towards the cell with highest ranking, but I will randomly deviate from this pattern on occasion so that I can explore some uncharted territory.

Translating these ideas into code is very straightforward, especially if you treat the underlying mathematical formulas as a black box:

module AntSim
  class Optimizer
    # ...

    def seek_food
      if here.food > 0 && (! here.home)
        :take_food
      elsif ahead.food > 0 && (! ahead.home ) && (! ahead.ant )
        :move_forward
      else
        food_ranking = rank_by { |cell| cell.food }
        pher_ranking = rank_by { |cell| cell.food_pheremone }

        ranks = combined_ranks(food_ranking, pher_ranking)
        follow_trail(ranks)
      end
    end

    def seek_home
      if here.home
        :drop_food
      elsif ahead.home && (! ahead.ant)
        :move_forward
      else
        home_ranking = rank_by { |cell| cell.home ? 1 : 0 }
        pher_ranking = rank_by { |cell| cell.home_pheremone }

        ranks = combined_ranks(home_ranking, pher_ranking)
        follow_trail(ranks)
      end
    end

    def follow_trail(ranks)
      choice = wrand([ ahead.ant ? 0 : ranks[ahead],
                       ranks[ahead_left],
                       ranks[ahead_right]])

      [:move_forward, :turn_left, :turn_right][choice]
    end
    

    # ...
  end
end
If you understand the general idea behind this algorithm, don’t worry about the exact computations that the Optimizer uses unless you are planning on researching Ant Colony Optimization in much greater detail. While I understand what my own code is doing, I’ll admit that I mostly cargo-cult copied the probabilistic methods from Rich Hickey’s simulator while sprinkling in a few minor tweaks here and there. That said, if you want to see exactly how I hacked things together, feel free to check out the full Optimizer class definition.

What I personally find much more interesting than the nuts and bolt of how this algorithm works is to think about why it works.

How the hive mind emerges
As we discussed in the previous section, ants are attracted to pheromone, and that makes them more likely to follow the trails left behind by other ants than they are to venture out on their own. However, when ants first start exploring a new space, there are no trails to follow and so they are forced to wander around randomly until a food source is found.

Generally speaking, ants that take a shorter path from the nest to a food source will arrive there sooner than ants that take a longer path. If they follow their own pheromone trail back to the nest, they will also return home sooner than those who are traversing longer paths. By the time ants who have taken a longer path return home, the ants on the shortest paths have already went back out in search of additional food, which increases the pheromone levels on their trails.

This process on its own would bias the ant colony to prefer shorter paths over longer ones, but the optimization would be somewhat sluggish and might tend to produce solutions that work well locally but aren’t nearly as attractive globally. To get better results, the system needs a bit of entropy thrown into the mix.

Because the behavior of ants has a certain amount of randomness to it, the occasional deviation from established paths are fairly common. Even if the fluctuations are small, each tiny shortcut that allows an ant to get between two points along a path in a shorter amount of time ultimately contributes to finding an optimal solution. This means that even an ant who goes wildly off course and starves to death nowhere near the nest can make a meaningful contribution to the colony if even some tiny segment of its path serves to shorten an existing well-worn trail.

When you add in the fact that pheromones are volatile and tend to evaporate over time, an upper limit emerges for how much a bad path or a local optimization can influence the colony’s decision making. Evaporation is also a key part of what allows the ants to change course when a food source is exhausted, or an obstacle stands in the way of an established path.

Pheromone decay is something that can be modeled in many ways, but the easiest way of simulating it is to gradually reduce the pheromone at every cell in the world on a regular interval. For an example of this approach, check out Simulator#evaporate:

module AntSim
  class Simulator
    def evaporate
      world.each do |cell, (x,y)| 
        cell.home_pheremone *= EVAP_RATE 
        cell.food_pheremone *= EVAP_RATE
      end
    end
  end
end
So if you take the basic positive feedback loop caused by pheromone attraction and mix in a bit of probabilistic exploration and the gradual evaporation of trails, you end up with a fairly robust optimization process. It truly is remarkable that these basic factors can combine to create a very effective search heuristic, especially when you consider the fact that what we’ve discussed here is only a crude approximation of the tip of the iceberg when it comes to Ant Colony Optimization.

Reflections
Emergent behaviors in computing problems have always fascinated me, even though I have not spent nearly enough time studying them to understand them well. I feel similarly about a lot of other things in life, ranging from the board game Go, to the spread of memes throughout communities both online and offline.

There is something deep and almost spiritual in the realization that the extremely complex behaviors can emerge from very simple systems with very few rules, and a complete lack of central organization. It forces us to call into question everything we experience and to wonder whether there is some elegant explanation for it all!

Practicing Ruby is a Practicing Developer project.
All articles on this website are independently published, open source, and advertising-free.


Want to keep reading? Check out the archives for more.
(Or see more from Gregory at practicingdeveloper.com)



https://en.wikipedia.org/wiki/Ant_colony_optimization_algorithms
In computer science and operations research, the ant colony optimization algorithm (ACO) is a probabilistic technique for solving computational problems that can be reduced to finding good paths through graphs. Artificial ants represent multi-agent methods inspired by the behavior of real ants. The pheromone-based communication of biological ants is often the predominant paradigm used.[2] Combinations of artificial ants and local search algorithms have become a preferred method for numerous optimization tasks involving some sort of graph, e.g., vehicle routing and internet routing.

As an example, ant colony optimization[3] is a class of optimization algorithms modeled on the actions of an ant colony.[4] Artificial 'ants' (e.g. simulation agents) locate optimal solutions by moving through a parameter space representing all possible solutions. Real ants lay down pheromones to direct each other to resources while exploring their environment. The simulated 'ants' similarly record their positions and the quality of their solutions, so that in later simulation iterations more ants locate better solutions.[5] One variation on this approach is the bees algorithm, which is more analogous to the foraging patterns of the honey bee, another social insect.

This algorithm is a member of the ant colony algorithms family, in swarm intelligence methods, and it constitutes some metaheuristic optimizations. Initially proposed by Marco Dorigo in 1992 in his PhD thesis,[6][7] the first algorithm was aiming to search for an optimal path in a graph, based on the behavior of ants seeking a path between their colony and a source of food. The original idea has since diversified to solve a wider class of numerical problems, and as a result, several problems have emerged, drawing on various aspects of the behavior of ants. From a broader perspective, ACO performs a model-based search[8] and shares some similarities with estimation of distribution algorithms.

Overview
In the natural world, ants of some species (initially) wander randomly, and upon finding food return to their colony while laying down pheromone trails. If other ants find such a path, they are likely to stop travelling at random and instead follow the trail, returning and reinforcing it if they eventually find food (see Ant communication).[9]

Over time, however, the pheromone trail starts to evaporate, thus reducing its attractive strength. The more time it takes for an ant to travel down the path and back again, the more time the pheromones have to evaporate. A short path, by comparison, is marched over more frequently, and thus the pheromone density becomes higher on shorter paths than longer ones. Pheromone evaporation also has the advantage of avoiding the convergence to a locally optimal solution. If there were no evaporation at all, the paths chosen by the first ants would tend to be excessively attractive to the following ones. In that case, the exploration of the solution space would be constrained. The influence of pheromone evaporation in real ant systems is unclear, but it is very important in artificial systems.[10]

The overall result is that when one ant finds a good (i.e., short) path from the colony to a food source, other ants are more likely to follow that path, and positive feedback eventually leads to many ants following a single path. The idea of the ant colony algorithm is to mimic this behavior with "simulated ants" walking around the graph representing the problem to be solved.

Ambient networks of intelligent objects
New concepts are required since "intelligence" is no longer centralized but can be found throughout all minuscule objects. Anthropocentric concepts have been known to lead to the production of IT systems in which data processing, control units and calculating power are centralized. These centralized units have continually increased their performance and can be compared to the human brain. The model of the brain has become the ultimate vision of computers. Ambient networks of intelligent objects and, sooner or later, a new generation of information systems that are even more diffused and based on nanotechnology, will profoundly change this concept. Small devices that can be compared to insects do not possess a high intelligence on their own. Indeed, their intelligence can be classed as fairly limited. It is, for example, impossible to integrate a high performance calculator with the power to solve any kind of mathematical problem into a biochip that is implanted into the human body or integrated in an intelligent tag designed to trace commercial articles. However, once those objects are interconnected they develop a form of intelligence that can be compared to a colony of ants or bees. In the case of certain problems, this type of intelligence can be superior to the reasoning of a centralized system similar to the brain.[11]

Nature offers several examples of how minuscule organisms, if they all follow the same basic rule, can create a form of collective intelligence on the macroscopic level. Colonies of social insects perfectly illustrate this model which greatly differs from human societies. This model is based on the cooperation of independent units with simple and unpredictable behavior.[12] They move through their surrounding area to carry out certain tasks and only possess a very limited amount of information to do so. A colony of ants, for example, represents numerous qualities that can also be applied to a network of ambient objects. Colonies of ants have a very high capacity to adapt themselves to changes in the environment, as well as great strength in dealing with situations where one individual fails to carry out a given task. This kind of flexibility would also be very useful for mobile networks of objects which are perpetually developing. Parcels of information that move from a computer to a digital object behave in the same way as ants would do. They move through the network and pass from one node to the next with the objective of arriving at their final destination as quickly as possible.[13]

Artificial pheromone system
Pheromone-based communication is one of the most effective ways of communication which is widely observed in nature. Pheromone is used by social insects such as bees, ants and termites; both for inter-agent and agent-swarm communications. Due to its feasibility, artificial pheromones have been adopted in multi-robot and swarm robotic systems. Pheromone-based communication was implemented by different means such as chemical [14][15][16] or physical (RFID tags,[17] light,[18][19][20][21] sound[22]) ways. However, those implementations were not able to replicate all the aspects of pheromones as seen in nature.

Using projected light was presented in a 2007 IEEE paper by Garnier, Simon, et al. as an experimental setup to study pheromone-based communication with micro autonomous robots.[23] Another study presented a system in which pheromones were implemented via a horizontal LCD screen on which the robots moved, with the robots having downward facing light sensors to register the patterns beneath them.[24][25]

Algorithm and formula
In the ant colony optimization algorithms, an artificial ant is a simple computational agent that searches for good solutions to a given optimization problem. To apply an ant colony algorithm, the optimization problem needs to be converted into the problem of finding the shortest path on a weighted graph. In the first step of each iteration, each ant stochastically constructs a solution, i.e. the order in which the edges in the graph should be followed. In the second step, the paths found by the different ants are compared. The last step consists of updating the pheromone levels on each edge.

procedure ACO_MetaHeuristic is
    while not terminated do
        generateSolutions()
        daemonActions()
        pheromoneUpdate()
    repeat
end procedure
Edge selection
Each ant needs to construct a solution to move through the graph. To select the next edge in its tour, an ant will consider the length of each edge available from its current position, as well as the corresponding pheromone level. At each step of the algorithm, each ant moves from a state 
x
{\displaystyle x} to state 
y
{\displaystyle y}, corresponding to a more complete intermediate solution. Thus, each ant 
k
{\displaystyle k} computes a set 
A
k
(
x
)
{\displaystyle A_{k}(x)} of feasible expansions to its current state in each iteration, and moves to one of these in probability. For ant 
k
{\displaystyle k}, the probability 
p
x
y
k
{\displaystyle p_{xy}^{k}} of moving from state 
x
{\displaystyle x} to state 
y
{\displaystyle y} depends on the combination of two values, the attractiveness 
η
x
y
{\displaystyle \eta _{xy}} of the move, as computed by some heuristic indicating the a priori desirability of that move and the trail level 
τ
x
y
{\displaystyle \tau _{xy}} of the move, indicating how proficient it has been in the past to make that particular move. The trail level represents a posteriori indication of the desirability of that move.

In general, the 
k
{\displaystyle k}th ant moves from state 
x
{\displaystyle x} to state 
y
{\displaystyle y} with probability

p
x
y
k
=
(
τ
x
y
α
)
(
η
x
y
β
)
∑
z
∈
a
l
l
o
w
e
d
y
(
τ
x
z
α
)
(
η
x
z
β
)
{\displaystyle p_{xy}^{k}={\frac {(\tau _{xy}^{\alpha })(\eta _{xy}^{\beta })}{\sum _{z\in \mathrm {allowed} _{y}}(\tau _{xz}^{\alpha })(\eta _{xz}^{\beta })}}}
where 
τ
x
y
{\displaystyle \tau _{xy}} is the amount of pheromone deposited for transition from state 
x
{\displaystyle x} to 
y
{\displaystyle y}, 
α
{\displaystyle \alpha } ≥ 0 is a parameter to control the influence of 
τ
x
y
{\displaystyle \tau _{xy}}, 
η
x
y
{\displaystyle \eta _{xy}} is the desirability of state transition 
x
y
{\displaystyle xy} (a priori knowledge, typically 
1
/
d
x
y
{\displaystyle 1/d_{xy}}, where 
d
{\displaystyle d} is the distance) and 
β
{\displaystyle \beta } ≥ 1 is a parameter to control the influence of 
η
x
y
{\displaystyle \eta _{xy}}. 
τ
x
z
{\displaystyle \tau _{xz}} and 
η
x
z
{\displaystyle \eta _{xz}} represent the trail level and attractiveness for the other possible state transitions.

Pheromone update
Trails are usually updated when all ants have completed their solution, increasing or decreasing the level of trails corresponding to moves that were part of "good" or "bad" solutions, respectively. An example of a global pheromone updating rule is now

τ
x
y
←
(
1
−
ρ
)
τ
x
y
+
∑
k
m
Δ
τ
x
y
k
{\displaystyle \tau _{xy}\leftarrow (1-\rho )\tau _{xy}+\sum _{k}^{m}\Delta \tau _{xy}^{k}}
where 
τ
x
y
{\displaystyle \tau _{xy}} is the amount of pheromone deposited for a state transition 
x
y
{\displaystyle xy}, 
ρ
{\displaystyle \rho } is the pheromone evaporation coefficient, 
m
{\displaystyle m} is the number of ants and 
Δ
τ
x
y
k
{\displaystyle \Delta \tau _{xy}^{k}} is the amount of pheromone deposited by 
k
{\displaystyle k}th ant, typically given for a TSP problem (with moves corresponding to arcs of the graph) by

Δ
τ
x
y
k
=
{
Q
/
L
k
if ant 
k
 uses curve 
x
y
 in its tour
0
otherwise
{\displaystyle \Delta \tau _{xy}^{k}={\begin{cases}Q/L_{k}&{\mbox{if ant }}k{\mbox{ uses curve }}xy{\mbox{ in its tour}}\\0&{\mbox{otherwise}}\end{cases}}}
where 
L
k
{\displaystyle L_{k}} is the cost of the 
k
{\displaystyle k}th ant's tour (typically length) and 
Q
{\displaystyle Q} is a constant.

Common extensions
Here are some of the most popular variations of ACO algorithms.

Ant system (AS)
The ant system is the first ACO algorithm. This algorithm corresponds to the one presented above. It was developed by Dorigo.[26]

Ant colony system (ACS)
In the ant colony system algorithm, the original ant system was modified in three aspects:

The edge selection is biased towards exploitation (i.e. favoring the probability of selecting the shortest edges with a large amount of pheromone);
While building a solution, ants change the pheromone level of the edges they are selecting by applying a local pheromone updating rule;
At the end of each iteration, only the best ant is allowed to update the trails by applying a modified global pheromone updating rule.[27]
Elitist ant system
In this algorithm, the global best solution deposits pheromone on its trail after every iteration (even if this trail has not been revisited), along with all the other ants. The elitist strategy has as its objective directing the search of all ants to construct a solution to contain links of the current best route.

Max-min ant system (MMAS)
This algorithm controls the maximum and minimum pheromone amounts on each trail. Only the global best tour or the iteration best tour are allowed to add pheromone to its trail. To avoid stagnation of the search algorithm, the range of possible pheromone amounts on each trail is limited to an interval [τmax,τmin]. All edges are initialized to τmax to force a higher exploration of solutions. The trails are reinitialized to τmax when nearing stagnation.[28]

Rank-based ant system (ASrank)
All solutions are ranked according to their length. Only a fixed number of the best ants in this iteration are allowed to update their trials. The amount of pheromone deposited is weighted for each solution, such that solutions with shorter paths deposit more pheromone than the solutions with longer paths.

Parallel ant colony optimization (PACO)
An ant colony system (ACS) with communication strategies is developed. The artificial ants are partitioned into several groups. Seven communication methods for updating the pheromone level between groups in ACS are proposed and work on the traveling salesman problem. [29]

Continuous orthogonal ant colony (COAC)
The pheromone deposit mechanism of COAC is to enable ants to search for solutions collaboratively and effectively. By using an orthogonal design method, ants in the feasible domain can explore their chosen regions rapidly and efficiently, with enhanced global search capability and accuracy. The orthogonal design method and the adaptive radius adjustment method can also be extended to other optimization algorithms for delivering wider advantages in solving practical problems.[30]

Recursive ant colony optimization
It is a recursive form of ant system which divides the whole search domain into several sub-domains and solves the objective on these subdomains.[31] The results from all the subdomains are compared and the best few of them are promoted for the next level. The subdomains corresponding to the selected results are further subdivided and the process is repeated until an output of desired precision is obtained. This method has been tested on ill-posed geophysical inversion problems and works well.[32]

Convergence
For some versions of the algorithm, it is possible to prove that it is convergent (i.e., it is able to find the global optimum in finite time). The first evidence of convergence for an ant colony algorithm was made in 2000, the graph-based ant system algorithm, and later on for the ACS and MMAS algorithms. Like most metaheuristics, it is very difficult to estimate the theoretical speed of convergence. A performance analysis of a continuous ant colony algorithm with respect to its various parameters (edge selection strategy, distance measure metric, and pheromone evaporation rate) showed that its performance and rate of convergence are sensitive to the chosen parameter values, and especially to the value of the pheromone evaporation rate.[33] In 2004, Zlochin and his colleagues[8] showed that ACO-type algorithms are closely related to stochastic gradient descent, Cross-entropy method and estimation of distribution algorithm. They proposed an umbrella term "Model-based search" to describe this class of metaheuristics.

Applications

Knapsack problem: The ants prefer the smaller drop of honey over the more abundant, but less nutritious, sugar
Ant colony optimization algorithms have been applied to many combinatorial optimization problems, ranging from quadratic assignment to protein folding or routing vehicles and a lot of derived methods have been adapted to dynamic problems in real variables, stochastic problems, multi-targets and parallel implementations. It has also been used to produce near-optimal solutions to the travelling salesman problem. They have an advantage over simulated annealing and genetic algorithm approaches of similar problems when the graph may change dynamically; the ant colony algorithm can be run continuously and adapt to changes in real time. This is of interest in network routing and urban transportation systems.

The first ACO algorithm was called the ant system[26] and it was aimed to solve the travelling salesman problem, in which the goal is to find the shortest round-trip to link a series of cities. The general algorithm is relatively simple and based on a set of ants, each making one of the possible round-trips along the cities. At each stage, the ant chooses to move from one city to another according to some rules:


Visualization of the ant colony algorithm applied to the travelling salesman problem. The green lines are the paths chosen by each ant. The blue lines are the paths it may take at each point. When the ant finishes, the pheromone levels are represented in red.
It must visit each city exactly once;
A distant city has less chance of being chosen (the visibility);
The more intense the pheromone trail laid out on an edge between two cities, the greater the probability that that edge will be chosen;
Having completed its journey, the ant deposits more pheromones on all edges it traversed, if the journey is short;
After each iteration, trails of pheromones evaporate.

Scheduling problem
Sequential ordering problem (SOP) [34]
Job-shop scheduling problem (JSP)[35]
Open-shop scheduling problem (OSP)[36][37]
Permutation flow shop problem (PFSP)[38]
Single machine total tardiness problem (SMTTP)[39]
Single machine total weighted tardiness problem (SMTWTP)[40][41][42]
Resource-constrained project scheduling problem (RCPSP)[43]
Group-shop scheduling problem (GSP)[44]
Single-machine total tardiness problem with sequence dependent setup times (SMTTPDST)[45]
Multistage flowshop scheduling problem (MFSP) with sequence dependent setup/changeover times[46]
Assembly sequence planning (ASP) problems[47]
Vehicle routing problem
Capacitated vehicle routing problem (CVRP)[48][49][50]
Multi-depot vehicle routing problem (MDVRP)[51]
Period vehicle routing problem (PVRP)[52]
Split delivery vehicle routing problem (SDVRP)[53]
Stochastic vehicle routing problem (SVRP)[54]
Vehicle routing problem with pick-up and delivery (VRPPD)[55][56]
Vehicle routing problem with time windows (VRPTW)[57][58][59][60]
Time dependent vehicle routing problem with time windows (TDVRPTW)[61]
Vehicle routing problem with time windows and multiple service workers (VRPTWMS)
Assignment problem
Quadratic assignment problem (QAP)[62]
Generalized assignment problem (GAP)[63][64]
Frequency assignment problem (FAP)[65]
Redundancy allocation problem (RAP)[66]
Set problem
Set cover problem (SCP)[67][68]
Partition problem (SPP)[69]
Weight constrained graph tree partition problem (WCGTPP)[70]
Arc-weighted l-cardinality tree problem (AWlCTP)[71]
Multiple knapsack problem (MKP)[72]
Maximum independent set problem (MIS)[73]
Device sizing problem in nanoelectronics physical design
Ant colony optimization (ACO) based optimization of 45 nm CMOS-based sense amplifier circuit could converge to optimal solutions in very minimal time.[74]
Ant colony optimization (ACO) based reversible circuit synthesis could improve efficiency significantly.[75]
Antennas optimization and synthesis

Loopback vibrators 10×10, synthesized by means of ACO algorithm[76]

Unloopback vibrators 10×10, synthesized by means of ACO algorithm[76]
To optimize the form of antennas, ant colony algorithms can be used. As example can be considered antennas RFID-tags based on ant colony algorithms (ACO),[77] loopback and unloopback vibrators 10×10[76]

Image processing
The ACO algorithm is used in image processing for image edge detection and edge linking.[78][79]

Edge detection:
The graph here is the 2-D image and the ants traverse from one pixel depositing pheromone. The movement of ants from one pixel to another is directed by the local variation of the image's intensity values. This movement causes the highest density of the pheromone to be deposited at the edges.

The following are the steps involved in edge detection using ACO:[80][81][82]

Step 1: Initialization. Randomly place 
K
{\displaystyle K} ants on the image 
I
M
1
M
2
{\displaystyle I_{M_{1}M_{2}}} where 
K
=
(
M
1
∗
M
2
)
1
2
{\displaystyle K=(M_{1}*M_{2})^{\tfrac {1}{2}}} . Pheromone matrix 
τ
(
i
,
j
)
{\displaystyle \tau _{(i,j)}} are initialized with a random value. The major challenge in the initialization process is determining the heuristic matrix.

There are various methods to determine the heuristic matrix. For the below example the heuristic matrix was calculated based on the local statistics: the local statistics at the pixel position 
(
i
,
j
)
{\displaystyle (i,j)}.

η
(
i
,
j
)
=
1
Z
∗
V
c
∗
I
(
i
,
j
)
,
{\displaystyle \eta _{(i,j)}={\tfrac {1}{Z}}*Vc*I_{(i,j)},}
where 
I
{\displaystyle I} is the image of size 
M
1
∗
M
2
{\displaystyle M_{1}*M_{2}},

Z
=
∑
i
=
1
:
M
1
∑
j
=
1
:
M
2
V
c
(
I
i
,
j
)
{\displaystyle Z=\sum _{i=1:M_{1}}\sum _{j=1:M_{2}}Vc(I_{i,j})}
is a normalization factor, and

V
c
(
I
i
,
j
)
=
f
(
|
I
(
i
−
2
,
j
−
1
)
−
I
(
i
+
2
,
j
+
1
)
|
+
|
I
(
i
−
2
,
j
+
1
)
−
I
(
i
+
2
,
j
−
1
)
|
+
|
I
(
i
−
1
,
j
−
2
)
−
I
(
i
+
1
,
j
+
2
)
|
+
|
I
(
i
−
1
,
j
−
1
)
−
I
(
i
+
1
,
j
+
1
)
|
+
|
I
(
i
−
1
,
j
)
−
I
(
i
+
1
,
j
)
|
+
|
I
(
i
−
1
,
j
+
1
)
−
I
(
i
−
1
,
j
−
1
)
|
+
|
I
(
i
−
1
,
j
+
2
)
−
I
(
i
−
1
,
j
−
2
)
|
+
|
I
(
i
,
j
−
1
)
−
I
(
i
,
j
+
1
)
|
)
{\displaystyle {\begin{aligned}Vc(I_{i,j})=&f\left(\left\vert I_{(i-2,j-1)}-I_{(i+2,j+1)}\right\vert +\left\vert I_{(i-2,j+1)}-I_{(i+2,j-1)}\right\vert \right.\\&+\left\vert I_{(i-1,j-2)}-I_{(i+1,j+2)}\right\vert +\left\vert I_{(i-1,j-1)}-I_{(i+1,j+1)}\right\vert \\&+\left\vert I_{(i-1,j)}-I_{(i+1,j)}\right\vert +\left\vert I_{(i-1,j+1)}-I_{(i-1,j-1)}\right\vert \\&+\left.\left\vert I_{(i-1,j+2)}-I_{(i-1,j-2)}\right\vert +\left\vert I_{(i,j-1)}-I_{(i,j+1)}\right\vert \right)\end{aligned}}}
f
(
⋅
)
{\displaystyle f(\cdot )} can be calculated using the following functions:

f
(
x
)
=
λ
x
,
for x ≥ 0; (1)
{\displaystyle f(x)=\lambda x,\quad {\text{for x ≥ 0; (1)}}}
f
(
x
)
=
λ
x
2
,
for x ≥ 0; (2)
{\displaystyle f(x)=\lambda x^{2},\quad {\text{for x ≥ 0; (2)}}}
f
(
x
)
=
{
sin
⁡
(
π
x
2
λ
)
,
for 0 ≤ x ≤
λ
; (3)
0
,
else
{\displaystyle f(x)={\begin{cases}\sin({\frac {\pi x}{2\lambda }}),&{\text{for 0 ≤ x ≤}}\lambda {\text{; (3)}}\\0,&{\text{else}}\end{cases}}}
f
(
x
)
=
{
π
x
sin
⁡
(
π
x
2
λ
)
,
for 0 ≤ x ≤
λ
; (4)
0
,
else
{\displaystyle f(x)={\begin{cases}\pi x\sin({\frac {\pi x}{2\lambda }}),&{\text{for 0 ≤ x ≤}}\lambda {\text{; (4)}}\\0,&{\text{else}}\end{cases}}}
The parameter 
λ
{\displaystyle \lambda } in each of above functions adjusts the functions' respective shapes.

Step 2: Construction process. The ant's movement is based on 4-connected pixels or 8-connected pixels. The probability with which the ant moves is given by the probability equation 
P
x
,
y
{\displaystyle P_{x,y}}

Step 3 and step 5: Update process. The pheromone matrix is updated twice. in step 3 the trail of the ant (given by 
τ
(
x
,
y
)
{\displaystyle \tau _{(x,y)}} ) is updated where as in step 5 the evaporation rate of the trail is updated which is given by:

τ
n
e
w
←
(
1
−
ψ
)
τ
o
l
d
+
ψ
τ
0
{\displaystyle \tau _{new}\leftarrow (1-\psi )\tau _{old}+\psi \tau _{0}},
where 
ψ
{\displaystyle \psi } is the pheromone decay coefficient 
0
<
τ
<
1
{\displaystyle 0<\tau <1}

Step 7: Decision process. Once the K ants have moved a fixed distance L for N iteration, the decision whether it is an edge or not is based on the threshold T on the pheromone matrix τ. Threshold for the below example is calculated based on Otsu's method.

Image edge detected using ACO: The images below are generated using different functions given by the equation (1) to (4).[83]


Edge linking:[84] ACO has also proven effective in edge linking algorithms.
Other applications
Bankruptcy prediction[85]
Classification[35]
Connection-oriented network routing[86]
Connectionless network routing[87][88]
Data mining[35][89][90][91]
Discounted cash flows in project scheduling[92]
Distributed information retrieval[93][94]
Energy and electricity network design[95]
Grid workflow scheduling problem[96]
Inhibitory peptide design for protein protein interactions[97]
Intelligent testing system[98]
Power electronic circuit design[99]
Protein folding[100][101][102]
System identification[103][104]
Definition difficulty

With an ACO algorithm, the shortest path in a graph, between two points A and B, is built from a combination of several paths.[105] It is not easy to give a precise definition of what algorithm is or is not an ant colony, because the definition may vary according to the authors and uses. Broadly speaking, ant colony algorithms are regarded as populated metaheuristics with each solution represented by an ant moving in the search space.[106] Ants mark the best solutions and take account of previous markings to optimize their search. They can be seen as probabilistic multi-agent algorithms using a probability distribution to make the transition between each iteration.[107] In their versions for combinatorial problems, they use an iterative construction of solutions.[108] According to some authors, the thing which distinguishes ACO algorithms from other relatives (such as algorithms to estimate the distribution or particle swarm optimization) is precisely their constructive aspect. In combinatorial problems, it is possible that the best solution eventually be found, even though no ant would prove effective. Thus, in the example of the travelling salesman problem, it is not necessary that an ant actually travels the shortest route: the shortest route can be built from the strongest segments of the best solutions. However, this definition can be problematic in the case of problems in real variables, where no structure of 'neighbours' exists. The collective behaviour of social insects remains a source of inspiration for researchers. The wide variety of algorithms (for optimization or not) seeking self-organization in biological systems has led to the concept of "swarm intelligence",[11] which is a very general framework in which ant colony algorithms fit.

Stigmergy algorithms
There is in practice a large number of algorithms claiming to be "ant colonies", without always sharing the general framework of optimization by canonical ant colonies.[109] In practice, the use of an exchange of information between ants via the environment (a principle called "stigmergy") is deemed enough for an algorithm to belong to the class of ant colony algorithms. This principle has led some authors to create the term "value" to organize methods and behavior based on search of food, sorting larvae, division of labour and cooperative transportation.[110]

Related methods
Genetic algorithms (GA)
These maintain a pool of solutions rather than just one. The process of finding superior solutions mimics that of evolution, with solutions being combined or mutated to alter the pool of solutions, with solutions of inferior quality being discarded.
Estimation of distribution algorithm (EDA)
An evolutionary algorithm that substitutes traditional reproduction operators by model-guided operators. Such models are learned from the population by employing machine learning techniques and represented as probabilistic graphical models, from which new solutions can be sampled[111][112] or generated from guided-crossover.[113][114]
Simulated annealing (SA)
A related global optimization technique which traverses the search space by generating neighboring solutions of the current solution. A superior neighbor is always accepted. An inferior neighbor is accepted probabilistically based on the difference in quality and a temperature parameter. The temperature parameter is modified as the algorithm progresses to alter the nature of the search.
Reactive search optimization
Focuses on combining machine learning with optimization, by adding an internal feedback loop to self-tune the free parameters of an algorithm to the characteristics of the problem, of the instance, and of the local situation around the current solution.
Tabu search (TS)
Similar to simulated annealing in that both traverse the solution space by testing mutations of an individual solution. While simulated annealing generates only one mutated solution, tabu search generates many mutated solutions and moves to the solution with the lowest fitness of those generated. To prevent cycling and encourage greater movement through the solution space, a tabu list is maintained of partial or complete solutions. It is forbidden to move to a solution that contains elements of the tabu list, which is updated as the solution traverses the solution space.
Artificial immune system (AIS)
Modeled on vertebrate immune systems.
Particle swarm optimization (PSO)
A swarm intelligence method.
Intelligent water drops (IWD)
A swarm-based optimization algorithm based on natural water drops flowing in rivers
Gravitational search algorithm (GSA)
A swarm intelligence method.
Ant colony clustering method (ACCM)
A method that make use of clustering approach, extending the ACO.
Stochastic diffusion search (SDS)
An agent-based probabilistic global search and optimization technique best suited to problems where the objective function can be decomposed into multiple independent partial-functions.
History

Chronology of ACO algorithms
Chronology of ant colony optimization algorithms.

1959, Pierre-Paul Grassé invented the theory of stigmergy to explain the behavior of nest building in termites;[115]
1983, Deneubourg and his colleagues studied the collective behavior of ants;[116]
1988, and Moyson Manderick have an article on self-organization among ants;[117]
1989, the work of Goss, Aron, Deneubourg and Pasteels on the collective behavior of Argentine ants, which will give the idea of ant colony optimization algorithms;[118]
1989, implementation of a model of behavior for food by Ebling and his colleagues;[119]
1991, M. Dorigo proposed the ant system in his doctoral thesis (which was published in 1992[7]). A technical report extracted from the thesis and co-authored by V. Maniezzo and A. Colorni[120] was published five years later;[26]
1994, Appleby and Steward of British Telecommunications Plc published the first application to telecommunications networks[121]
1995, Gambardella and Dorigo proposed ant-q,[122] the preliminary version of ant colony system as first extension of ant system;.[26]
1996, Gambardella and Dorigo proposed ant colony system [123]
1996, publication of the article on ant system;[26]
1997, Dorigo and Gambardella proposed ant colony system hybridized with local search;[27]
1997, Schoonderwoerd and his colleagues published an improved application to telecommunication networks;[124]
1998, Dorigo launches first conference dedicated to the ACO algorithms;[125]
1998, Stützle proposes initial parallel implementations;[126]
1999, Gambardella, Taillard and Agazzi proposed macs-vrptw, first multi ant colony system applied to vehicle routing problems with time windows,[57]
1999, Bonabeau, Dorigo and Theraulaz publish a book dealing mainly with artificial ants[127]
2000, special issue of the Future Generation Computer Systems journal on ant algorithms[128]
2000, Hoos and Stützle invent the max-min ant system;[28]
2000, first applications to the scheduling, scheduling sequence and the satisfaction of constraints;
2000, Gutjahr provides the first evidence of convergence for an algorithm of ant colonies[129]
2001, the first use of COA algorithms by companies (Eurobios and AntOptima);
2001, Iredi and his colleagues published the first multi-objective algorithm[130]
2002, first applications in the design of schedule, Bayesian networks;
2002, Bianchi and her colleagues suggested the first algorithm for stochastic problem;[131]
2004, Dorigo and Stützle publish the Ant Colony Optimization book with MIT Press [132]
2004, Zlochin and Dorigo show that some algorithms are equivalent to the stochastic gradient descent, the cross-entropy method and algorithms to estimate distribution[8]
2005, first applications to protein folding problems.
2012, Prabhakar and colleagues publish research relating to the operation of individual ants communicating in tandem without pheromones, mirroring the principles of computer network organization. The communication model has been compared to the Transmission Control Protocol.[133]
2016, first application to peptide sequence design.[97]
2017, successful integration of the multi-criteria decision-making method PROMETHEE into the ACO algorithm (HUMANT algorithm).[134]

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Ant sim ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Common Public License 1.0 (http://opensource.org/licenses/cpl.php)
;   which can be found in the file CPL.TXT at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;dimensions of square world
(def dim 80)
;number of ants = nants-sqrt^2
(def nants-sqrt 7)
;number of places with food
(def food-places 35)
;range of amount of food at a place
(def food-range 100)
;scale factor for pheromone drawing
(def pher-scale 20.0)
;scale factor for food drawing
(def food-scale 30.0)
;evaporation rate
(def evap-rate 0.99)

(def animation-sleep-ms 100)
(def ant-sleep-ms 40)
(def evap-sleep-ms 1000)

(def running true)

(defstruct cell :food :pher) ;may also have :ant and :home

;world is a 2d vector of refs to cells
(def world 
     (apply vector 
            (map (fn [_] 
                   (apply vector (map (fn [_] (ref (struct cell 0 0))) 
                                      (range dim)))) 
                 (range dim))))

(defn place [[x y]]
  (-> world (nth x) (nth y)))

(defstruct ant :dir) ;may also have :food

(defn create-ant 
  "create an ant at the location, returning an ant agent on the location"
  [loc dir]
    (sync nil
      (let [p (place loc)
            a (struct ant dir)]
        (alter p assoc :ant a)
        (agent loc))))

(def home-off (/ dim 4))
(def home-range (range home-off (+ nants-sqrt home-off)))

(defn setup 
  "places initial food and ants, returns seq of ant agents"
  []
  (sync nil
    (dotimes [i food-places]
      (let [p (place [(rand-int dim) (rand-int dim)])]
        (alter p assoc :food (rand-int food-range))))
    (doall
     (for [x home-range y home-range]
       (do
         (alter (place [x y]) 
                assoc :home true)
         (create-ant [x y] (rand-int 8)))))))

(defn bound 
  "returns n wrapped into range 0-b"
  [b n]
    (let [n (rem n b)]
      (if (neg? n) 
        (+ n b) 
        n)))

(defn wrand 
  "given a vector of slice sizes, returns the index of a slice given a
  random spin of a roulette wheel with compartments proportional to
  slices."
  [slices]
  (let [total (reduce + slices)
        r (rand total)]
    (loop [i 0 sum 0]
      (if (< r (+ (slices i) sum))
        i
        (recur (inc i) (+ (slices i) sum))))))

;dirs are 0-7, starting at north and going clockwise
;these are the deltas in order to move one step in given dir
(def dir-delta {0 [0 -1]
                1 [1 -1]
                2 [1 0]
                3 [1 1]
                4 [0 1]
                5 [-1 1]
                6 [-1 0]
                7 [-1 -1]})

(defn delta-loc 
  "returns the location one step in the given dir. Note the world is a torus"
  [[x y] dir]
    (let [[dx dy] (dir-delta (bound 8 dir))]
      [(bound dim (+ x dx)) (bound dim (+ y dy))]))

;(defmacro dosync [& body]
;  `(sync nil ~@body))

;ant agent functions
;an ant agent tracks the location of an ant, and controls the behavior of 
;the ant at that location

(defn turn 
  "turns the ant at the location by the given amount"
  [loc amt]
    (dosync
     (let [p (place loc)
           ant (:ant @p)]
       (alter p assoc :ant (assoc ant :dir (bound 8 (+ (:dir ant) amt))))))
    loc)

(defn move 
  "moves the ant in the direction it is heading. Must be called in a
  transaction that has verified the way is clear"
  [loc]
     (let [oldp (place loc)
           ant (:ant @oldp)
           newloc (delta-loc loc (:dir ant))
           p (place newloc)]
         ;move the ant
       (alter p assoc :ant ant)
       (alter oldp dissoc :ant)
         ;leave pheromone trail
       (when-not (:home @oldp)
         (alter oldp assoc :pher (inc (:pher @oldp))))
       newloc))

(defn take-food [loc]
  "Takes one food from current location. Must be called in a
  transaction that has verified there is food available"
  (let [p (place loc)
        ant (:ant @p)]    
    (alter p assoc 
           :food (dec (:food @p))
           :ant (assoc ant :food true))
    loc))

(defn drop-food [loc]
  "Drops food at current location. Must be called in a
  transaction that has verified the ant has food"
  (let [p (place loc)
        ant (:ant @p)]    
    (alter p assoc 
           :food (inc (:food @p))
           :ant (dissoc ant :food))
    loc))

(defn rank-by 
  "returns a map of xs to their 1-based rank when sorted by keyfn"
  [keyfn xs]
  (let [sorted (sort-by (comp float keyfn) xs)]
    (reduce (fn [ret i] (assoc ret (nth sorted i) (inc i)))
            {} (range (count sorted)))))

(defn behave 
  "the main function for the ant agent"
  [loc]
  (let [p (place loc)
        ant (:ant @p)
        ahead (place (delta-loc loc (:dir ant)))
        ahead-left (place (delta-loc loc (dec (:dir ant))))
        ahead-right (place (delta-loc loc (inc (:dir ant))))
        places [ahead ahead-left ahead-right]]
    (. Thread (sleep ant-sleep-ms))
    (dosync
     (when running
       (send-off *agent* #'behave))
     (if (:food ant)
       ;going home
       (cond 
        (:home @p)                              
          (-> loc drop-food (turn 4))
        (and (:home @ahead) (not (:ant @ahead))) 
          (move loc)
        :else
          (let [ranks (merge-with + 
                        (rank-by (comp #(if (:home %) 1 0) deref) places)
                        (rank-by (comp :pher deref) places))]
          (([move #(turn % -1) #(turn % 1)]
            (wrand [(if (:ant @ahead) 0 (ranks ahead)) 
                    (ranks ahead-left) (ranks ahead-right)]))
           loc)))
       ;foraging
       (cond 
        (and (pos? (:food @p)) (not (:home @p))) 
          (-> loc take-food (turn 4))
        (and (pos? (:food @ahead)) (not (:home @ahead)) (not (:ant @ahead)))
          (move loc)
        :else
          (let [ranks (merge-with + 
                                  (rank-by (comp :food deref) places)
                                  (rank-by (comp :pher deref) places))]
          (([move #(turn % -1) #(turn % 1)]
            (wrand [(if (:ant @ahead) 0 (ranks ahead)) 
                    (ranks ahead-left) (ranks ahead-right)]))
           loc)))))))

(defn evaporate 
  "causes all the pheromones to evaporate a bit"
  []
  (dorun 
   (for [x (range dim) y (range dim)]
     (dosync 
      (let [p (place [x y])]
        (alter p assoc :pher (* evap-rate (:pher @p))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; UI ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(import 
 '(java.awt Color Graphics Dimension)
 '(java.awt.image BufferedImage)
 '(javax.swing JPanel JFrame))

;pixels per world cell
(def scale 5)

(defn fill-cell [#^Graphics g x y c]
  (doto g
    (.setColor c)
    (.fillRect (* x scale) (* y scale) scale scale)))

(defn render-ant [ant #^Graphics g x y]
  (let [black (. (new Color 0 0 0 255) (getRGB))
        gray (. (new Color 100 100 100 255) (getRGB))
        red (. (new Color 255 0 0 255) (getRGB))
        [hx hy tx ty] ({0 [2 0 2 4] 
                        1 [4 0 0 4] 
                        2 [4 2 0 2] 
                        3 [4 4 0 0] 
                        4 [2 4 2 0] 
                        5 [0 4 4 0] 
                        6 [0 2 4 2] 
                        7 [0 0 4 4]}
                       (:dir ant))]
    (doto g
      (.setColor (if (:food ant) 
                  (new Color 255 0 0 255) 
                  (new Color 0 0 0 255)))
      (.drawLine (+ hx (* x scale)) (+ hy (* y scale)) 
                (+ tx (* x scale)) (+ ty (* y scale))))))

(defn render-place [g p x y]
  (when (pos? (:pher p))
    (fill-cell g x y (new Color 0 255 0 
                          (int (min 255 (* 255 (/ (:pher p) pher-scale)))))))
  (when (pos? (:food p))
    (fill-cell g x y (new Color 255 0 0 
                          (int (min 255 (* 255 (/ (:food p) food-scale)))))))
  (when (:ant p)
    (render-ant (:ant p) g x y)))

(defn render [g]
  (let [v (dosync (apply vector (for [x (range dim) y (range dim)] 
                                   @(place [x y]))))
        img (new BufferedImage (* scale dim) (* scale dim) 
                 (. BufferedImage TYPE_INT_ARGB))
        bg (. img (getGraphics))]
    (doto bg
      (.setColor (. Color white))
      (.fillRect 0 0 (. img (getWidth)) (. img (getHeight))))
    (dorun 
     (for [x (range dim) y (range dim)]
       (render-place bg (v (+ (* x dim) y)) x y)))
    (doto bg
      (.setColor (. Color blue))
      (.drawRect (* scale home-off) (* scale home-off) 
                 (* scale nants-sqrt) (* scale nants-sqrt)))
    (. g (drawImage img 0 0 nil))
    (. bg (dispose))))

(def panel (doto (proxy [JPanel] []
                        (paint [g] (render g)))
             (.setPreferredSize (new Dimension 
                                     (* scale dim) 
                                     (* scale dim)))))

(def frame (doto (new JFrame) (.add panel) .pack .show))

(def animator (agent nil))

(defn animation [x]
  (when running
    (send-off *agent* #'animation))
  (. panel (repaint))
  (. Thread (sleep animation-sleep-ms))
  nil)

(def evaporator (agent nil))

(defn evaporation [x]
  (when running
    (send-off *agent* #'evaporation))
  (evaporate)
  (. Thread (sleep evap-sleep-ms))
  nil)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; use ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; (comment
;demo
;; (load-file "/Users/rich/dev/clojure/ants.clj")
(def ants (setup))
(send-off animator animation)
(dorun (map #(send-off % behave) ants))
(send-off evaporator evaporation)

;; )




--- ruby version---

basedir = File.join(File.dirname(__FILE__), "ant_sim")

%w[ant cell world actor simulator optimizer visualization].each do |lib|
  require "#{basedir}/#{lib}"
end

module AntSim
  class World
    def initialize(world_size)
      self.size = world_size
      self.data = size.times.map { size.times.map { Cell.new(0,0,0) } }
    end

    attr_reader :size

    def [](location)
      x,y = location

      data[x][y]
    end

    def sample
      data[rand(size)][rand(size)]
    end

    def each
      data.each_with_index do |col,x| 
        col.each_with_index do |cell, y| 
          yield [cell, [x, y]]
        end
      end
    end

    private

    attr_accessor :data
    attr_writer   :size
  end
end

module AntSim
  class Visualization
    include Java

    import java.awt.Color
    import java.awt.Graphics
    import java.awt.BasicStroke
    import java.awt.Dimension

    import java.awt.image.BufferedImage
    import javax.swing.JPanel
    import javax.swing.JFrame

    class Panel < JPanel
      attr_accessor :interface, :simulator

      def paint(g)
        interface.render(g, simulator)
      end
    end

    SCALE              = 10
    PHEREMONE_SCALE    = 10.0
    FOOD_SCALE         = 30.0
    EVAPORATION_DELAY  = 0.2

    def self.run
      new.run
    end

    def run
      sim = Simulator.new
      ui  = self

      food_cells = []

      sim.world.each do |cell, (x,y)|
        food_cells << [[x,y], cell] if cell.food > 0
      end

      panel = Panel.new
      panel.simulator = sim
      panel.interface = ui
      
      panel.setPreferredSize(Dimension.new(SCALE * Simulator::DIMENSIONS,
                                           SCALE * Simulator::DIMENSIONS))
      frame = JFrame.new
      frame.add(panel)
      frame.pack
      frame.show

      t = Time.now

      loop do
        if Time.now - t > EVAPORATION_DELAY
          sim.evaporate
          t = Time.now
        end

        sim.iterate

        panel.repaint
      end
    end

    def fill_cell(g, x, y, c)
      g.setColor(c)
      g.fillRect(x * SCALE, y * SCALE, SCALE, SCALE)
    end

    def render_ant(ant, g, x, y)
      black = Color.new(0,0,0,255).getRGB
      gray  = Color.new(100,100,100,255).getRGB
      red   = Color.new(255,  0,  0,255).getRGB

      hx, hy, tx, ty = [[2, 0, 2, 4], [4, 0, 0, 4], [4, 2, 0, 2], [4, 4, 0, 0],
                        [2, 4, 2, 0], [0, 4, 4, 0], [0, 2, 4, 2], [0, 0, 4, 4]][ant.direction]

      g.setStroke(BasicStroke.new(3))
      g.setColor(ant.food ? Color.new(255, 0, 0, 255) : Color.new(0, 0, 0, 255))
      g.drawLine(hx + x * SCALE, hy + y * SCALE, tx + x * SCALE, ty + y * SCALE)
    end

    def render_place(g, cell, x, y)
      if cell.food_pheremone > 0
        fill_cell(g, x, y, Color.new(0,0,255, [255 * (cell.food_pheremone / PHEREMONE_SCALE), 255].min.to_i))
      elsif cell.home_pheremone > 0
        fill_cell(g, x, y, Color.new(0, 255, 0, [255 * (cell.home_pheremone / PHEREMONE_SCALE), 255].min.to_i))
      end

      if cell.food > 0
        fill_cell(g, x, y, Color.new(255, 0, 0, [255 * (cell.food / FOOD_SCALE), 255].min.to_i))
      end

      if ant = cell.ant
        render_ant(ant, g, x, y)
      end
    end

    def render(g, sim)
      dim = Simulator::DIMENSIONS

      img = BufferedImage.new(SCALE * dim, 
                              SCALE * dim,
                              BufferedImage::TYPE_INT_ARGB)

      bg  = img.getGraphics

      bg.setColor(Color.white)
      bg.fillRect(0,0, img.getWidth, img.getHeight)

      sim.world.each do |cell, (x,y)|
        render_place(bg, cell, x, y)
      end

      bg.setColor(Color.blue)
      bg.drawRect(SCALE * Simulator::HOME_OFFSET, SCALE * Simulator::HOME_OFFSET,
                  SCALE * Simulator::NANTS_SQRT,  SCALE * Simulator::NANTS_SQRT)

      g.drawImage(img, 0, 0, nil)
      bg.dispose
    end
  end
end
module AntSim
  class Simulator
    DIMENSIONS  = 80
    FOOD_PLACES = 35
    FOOD_RANGE  = 50
    HOME_OFFSET = DIMENSIONS / 4
    NANTS_SQRT  = 7
    HOME_RANGE  = HOME_OFFSET ... HOME_OFFSET + NANTS_SQRT

    EVAP_RATE    = 0.95
    ANT_SLEEP    = 0.005

    def initialize
      self.world  = World.new(DIMENSIONS)
      self.actors = []

      FOOD_PLACES.times do
        world.sample.food = FOOD_RANGE
      end

      HOME_RANGE.to_a.product(HOME_RANGE.to_a).map do |x,y|
        ant = Ant.new(rand(8), [x,y])
       
        world[[x,y]].home = true
        world[[x,y]].ant  = ant

        actors << Actor.new(world, ant)
      end
    end

    attr_reader :world, :actors

    def iterate
      actors.each do |actor|
        optimizer = Optimizer.new(actor.here, actor.nearby_places)
        
        if actor.foraging?
          action = optimizer.seek_food
        else
          action = optimizer.seek_home
        end

        case action
        when :drop_food
          actor.drop_food.mark_food_trail.turn(4)
        when :take_food
          actor.take_food.mark_home_trail.turn(4)
        when :move_forward
          actor.move
        when :turn_left
          actor.turn(-1)
        when :turn_right
          actor.turn(1)
        else
          raise NotImplementedError, action.inspect
        end
      end

      sleep ANT_SLEEP
    end

    def evaporate
      world.each do |cell, (x,y)| 
        cell.home_pheremone *= EVAP_RATE 
        cell.food_pheremone *= EVAP_RATE
      end
    end

    private

    attr_writer :world, :actors
  end
end
module AntSim
  class Optimizer
    BEST_CHOICE_BONUS = 3

    def initialize(here, nearby_places)
      self.here          = here
      self.nearby_places = nearby_places

      self.ahead, self.ahead_left, self.ahead_right = nearby_places
    end

    attr_reader :here, :nearby_places, :ahead, :ahead_left, :ahead_right

    def seek_food
      if here.food > 0 && (! here.home)
        :take_food
      elsif ahead.food > 0 && (! ahead.home ) && (! ahead.ant )
        :move_forward
      else
        food_ranking = rank_by { |cell| cell.food }
        pher_ranking = rank_by { |cell| cell.food_pheremone }

        ranks = combined_ranks(food_ranking, pher_ranking)
        follow_trail(ranks)
      end
    end

    def seek_home
      if here.home
        :drop_food
      elsif ahead.home && (! ahead.ant)
        :move_forward
      else
        home_ranking = rank_by { |cell| cell.home ? 1 : 0 }
        pher_ranking = rank_by { |cell| cell.home_pheremone }

        ranks = combined_ranks(home_ranking, pher_ranking)
        follow_trail(ranks)
      end
    end

    def follow_trail(ranks)
      choice = wrand([ ahead.ant ? 0 : ranks[ahead],
                       ranks[ahead_left],
                       ranks[ahead_right]])

      [:move_forward, :turn_left, :turn_right][choice]
    end

    private

    attr_writer :here, :nearby_places, :ahead, :ahead_left, :ahead_right
    
    def combined_ranks(a,b)
      combined = a.merge(b) { |k,v|  a[k] + b[k] }
      top_k, _ = combined.max_by { |k,v| v }

      combined[top_k] *= BEST_CHOICE_BONUS

      combined
    end

    def rank_by(&keyfn)
      ranks  = Hash.new { |h,k| h[k] = 0 }
      sorted = nearby_places.sort_by { |e| keyfn.call(e).to_f }

      (0...sorted.length).each { |i| ranks[sorted[i]] = i + 1 }

      ranks
    end

    def wrand(slices)
      total = slices.reduce(:+)
      r     = rand(total)

      sum   = 0

      slices.each_with_index do |e,i|
        return i if r < sum + e
        
        sum  += e
      end
    end
  end
end
module AntSim
  class Cell
    def initialize(food, home_pheremone, food_pheremone)
      self.food = food 
      self.home_pheremone = home_pheremone
      self.food_pheremone = food_pheremone
    end

    attr_accessor :food, :home_pheremone, :food_pheremone, :ant, :home
  end
end

module AntSim
  class Ant
    def initialize(direction, location)
      self.direction = direction
      self.location  = location
    end

    attr_accessor :food, :direction, :location
  end
end

require "set"

module AntSim
  class Actor
    DIR_DELTA   = [[0, -1], [ 1, -1], [ 1, 0], [ 1,  1],
                   [0,  1], [-1,  1], [-1, 0], [-1, -1]]

    def initialize(world, ant)
      self.world   = world
      self.ant     = ant

      self.history = Set.new
    end

    attr_reader :ant

    def turn(amt)
      ant.direction = (ant.direction + amt) % 8

      self
    end

    def move
      history << here

      new_location = neighbor(ant.direction)

      ahead.ant = ant
      here.ant  = nil

      ant.location = new_location

      self
    end

    def drop_food
      here.food += 1
      ant.food   = false

      self
    end

    def take_food
      here.food -= 1
      ant.food   = true

      self
    end

    def mark_food_trail
      history.each do |old_cell|
        old_cell.food_pheremone += 1 unless old_cell.food > 0 
      end

      history.clear

      self
    end

    def mark_home_trail
      history.each do |old_cell|
        old_cell.home_pheremone += 1 unless old_cell.home
      end

      history.clear

      self
    end

    def foraging?
      !ant.food
    end

    def here
      world[ant.location]
    end

    def ahead
      world[neighbor(ant.direction)]
    end

    def ahead_left
      world[neighbor(ant.direction - 1)]
    end

    def ahead_right
      world[neighbor(ant.direction + 1)]
    end
    
    def nearby_places
      [ahead, ahead_left, ahead_right]
    end

    private

    def neighbor(direction)
      x,y = ant.location

      dx, dy = DIR_DELTA[direction % 8]

      [(x + dx) % world.size, (y + dy) % world.size]
    end

    attr_accessor :world, :history
    attr_writer   :ant
  end
end