import groovy.transform.Field

@Field def assignment = []
@Field int numberOfVars
@Field int numberOfClauses
@Field def clauses = []
@Field def pop_size = Integer.parseInt(args[2])
@Field def max_gens = Integer.parseInt(args[1])
@Field Random random = new Random();
@Field def population = []
@Field def fitness = []
@Field def top50 = []
@Field def children = []

def getData() {
    def cnfFile = new File(args[0])
    def lines = cnfFile.readLines()
    for (String l : lines) {
        l = l.trim()
        if (!l.startsWith("c")) {
            if (l.startsWith("p")) {
                def s = l.split(" ")
                numberOfVars = Integer.parseInt(s[2])
                numberOfClauses = Integer.parseInt(s[3])
                assignment = new Integer[numberOfVars + 1]
                assignment[0] = 0
            } else {
                def s = l.split(" ")
                def singleClause = []
                def itr = s.iterator()
                singleClause = itr.collectMany { Integer.parseInt(it) == 0 ? [] : [Integer.parseInt(it)] }
                singleClause = singleClause.unique()
                clauses << singleClause
            }
        }
    }
}

def getInitialPopulation() {
    if(Math.pow(2, numberOfVars) < pop_size) pop_size = Math.pow(2, numberOfVars)
    while (population.size() < pop_size) {//upper limit: population size
        def current = new Integer[numberOfVars + 1]
        current[0] = 0
        (1..numberOfVars).each {//upper limit: number of variables
            current[it] = random.nextInt(2) == 0 ? -1 : 1
        }
        population << current
        population.unique()
    }
}

def businessLogic() {
    println("c generating population")
    getInitialPopulation()
    println("c done generating initial population")
    for(def i = 1; i<=max_gens; i++) {
        checkFitnessOfPopulation()
        if (fitness.find { it == 0 } != null) {
            // you got the assignment man.. print and relax!!!
            printSat(population[fitness.findIndexOf { it == 0 }])
            return 1
        } else {
            println("c " + fitness)
            recreate()
            dropLast50OfTop50()
            addChildrenToPopulation()
        }
    }
    println("s UNSATISFIABLE")
}

def checkFitnessOfPopulation() {
    population.each {
        fitness[population.indexOf(it)] = numberOfUnsatClauses(it)
    }
}

def numberOfUnsatClauses(assign) {
    def count = 0
    clauses.each {
        ifClauseIsSat(it, assign) ? 0 : ++count
    }
    return count
}

def ifClauseIsSat(clause, assign) {
    return clause.any() {
        it * assign[Math.abs(it)] > 0
    }
}

def recreate() {
    children = []
    def topHalf = [fitness, population].transpose().sort().take(Math.toIntExact(Math.round(pop_size / 2)))
    top50 = topHalf.collect { it[1] }
    def superMom = Math.toIntExact(Math.round(numberOfVars / 3)) //size of first and last part
    for (def i = 0; i < top50.size() - 1; i += 2) {
        def female = top50[i]
        def male = top50[i + 1]
//        println(" c female " + female + " male " + male)
//        println("c supermom " + superMom)
        def child = [female.take(superMom), male[superMom..<male.size() - superMom], female.takeRight(superMom)]
        child = child.flatten()
        Math.random() > 0.5 ? mutate(child) : "Not mutating"
//        println("c child " + child)
        children << child
    }
}

def mutate(child) {
    child[random.nextInt(numberOfVars) + 1] *= -1
}

def dropLast50OfTop50() {
    (top50.size() - 1..children.size()).each {
        removePointer ->
            population.remove(population.findIndexOf { it == population[removePointer] })
    }
//    println("pops " + population)
}

def addChildrenToPopulation() {
    children.each {
        population << it
    }
}

def printSat(assign) {
    println("s SATISFIABLE")
    print("v ")
    (1..numberOfVars).each {
        print(assign[it]*it + " ")
    }
}

getData()
clauses.unique()
println(" c done")
businessLogic()
