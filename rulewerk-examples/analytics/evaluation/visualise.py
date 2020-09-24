import matplotlib.pyplot as plt

'''
Class that represents the results for a benchmark
The data is stored in array consisting of tuples in the format (system,task,instance,time)
'''
class Benchmark():

    '''
    Constructor.
    @param{str} name     the name of the benchmark
    @param{str} filename the name of the file with the data
    '''
    def __init__(self, name, filename):
        self.name = name
        self.data = []
        with open(filename) as f:
            for line in f:
                system, task, instance, time = [elem.strip() for elem in line.split(',')]
                if ':' in time:
                    mins, secs = time.split(':')
                    y = 60 * float(mins) + float(secs)
                else:
                    y = float(time.strip('s'))
                x = int(instance[instance.index('_')+1:])
                self.data.append((system,task,x,y))
        self.fig, self.ax = plt.subplots()

    '''
    Returns an array with the data for a specific task
    @param{str} wanted_task    the task to use as filter 
    @return{(str,int,float)[]} the filtered array
    '''
    def filterByTask(self, wanted_task):
        return [(system,x,y) for (system,task,x,y) in self.data if task == wanted_task ]
    
    '''
    Returns an array with the data for a specific system
    @param{str} wanted_system  the task to use as filter 
    @return{(str,int,float)[]} the filtered array
    '''
    def filterBySystem(self, wanted_system):
        return [(task,x,y) for (system,task,x,y) in self.data if system == wanted_system]
    
    '''
    Returns an array with the data for a specific system and task
    @param{str} wanted_system the task to use as filter 
    @param{str} wanted_task   the task to use as filter 
    @return{(int,float)[]}    the filtered array
    '''
    def filterBySystemAndTask(self, wanted_system, wanted_task):
        return [(x,y) for (system,task,x,y) in self.data if system == wanted_system and task == wanted_task]

    '''
    Set the data for the benchmark
    @param{(str,str,int,float)[]} data the data to set
    '''
    def setData(self, data):
        self.data = data

    '''
    Returns the data where the average of the time for (each system,task,instance) was computed
    @return{(str,str,int,float)[]} the averaged data
    '''
    def getAverage(self):
        # dictionary: (system, task, x) => (count(y), sum(y))
        d = {}
        for system,task,x,y in self.data:
            key = (system,task,x)
            if key in d.keys():
                c,s = d[key]
                d[key] = (c+1,s+y)
            else:
                d[key] = (1,y)
        return [(key[0], key[1], key[2], d[key][1]/d[key][0]) for key in d.keys()] 

    '''
    Computes and sets the average of the time.
    '''
    def average(self):
        self.data = self.getAverage()

    '''
    Gets a set of all systems used in the benchmark.
    @returns{set<str>} the set of systems
    '''
    def getSystems(self):
        return set([system for system,_,_,_ in self.data])

    '''
    Scatters the points for a given system and task
    @param{str} system the system to scatter for
    @param{str} task   the task to scatter for
    '''
    def scatter(self, system, task):
        xs = []
        ys = []
        for x,y in self.filterBySystemAndTask(system,task):
            xs.append(x)
            ys.append(y)
        self.ax.scatter(xs,ys,label=system)

    '''
    Sets general visual parameter and shows the plot.
    '''
    def show(self):
        self.ax.set_ylim(bottom=0)
        self.ax.set_xlim(left=0)

        self.ax.set_ylabel('Time [s]')
        self.ax.set_xlabel('Crossword size')
        self.ax.legend(loc='upper center', bbox_to_anchor=(0.5, 1), ncol=3, fancybox=True)

        plt.show(block=self.fig)

    
    def getTime(self, system, task):
        return [time for _,time in self.filter(system,task)]



if __name__ == '__main__':
    name = input('Benchmark name: ')
    filename = input('Filename: ')
    benchmark = Benchmark(name, filename)
    benchmark.average()
    print(benachmark.getTime('rulewerk','ground'))
    benchmark.scatter('rulewerk','ground')
    benchmark.scatter('gringo','ground')
    benchmark.show()

