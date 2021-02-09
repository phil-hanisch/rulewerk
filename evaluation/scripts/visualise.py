from matplotlib import rc
from matplotlib.markers import CARETLEFT, CARETRIGHT, CARETLEFTBASE, CARETRIGHTBASE
import matplotlib.pyplot as plt
import matplotlib as mpl
import time

mpl.use('pgf')


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
                x = int(instance[instance.index('e')+1:]) if 'size' in instance else instance
                self.data.append((system,task,x,y))
        self.fig, self.ax = plt.subplots()
        self.raw_data = self.data

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
        for system,task,x,y in self.raw_data:
            key = (system,task,x)
            if key in d.keys():
                c,s = d[key]
                d[key] = (c+1,s+y)
            else:
                d[key] = (1,y)
        return [(key[0], key[1], key[2], d[key][1]/d[key][0]) for key in d.keys()] 

    '''
    Returns the data where the minimum of the time for (each system,task,instance) is returned
    @return{(str,str,int,float)[]} the averaged data
    '''
    def getMinimum(self):
        # dictionary: (system, task, x) => min(y)
        d = {}
        for system,task,x,y in self.raw_data:
            key = (system,task,x)
            if key in d.keys():
                if y < d[key]:
                    d[key] = y
            else:
                d[key] = y
        return [(key[0], key[1], key[2], d[key]) for key in d.keys()] 

    '''
    Returns the data where the maximum of the time for (each system,task,instance) is returned
    @return{(str,str,int,float)[]} the averaged data
    '''
    def getMaximum(self):
        # dictionary: (system, task, x) => max(y)
        d = {}
        for system,task,x,y in self.raw_data:
            key = (system,task,x)
            if key in d.keys():
                if y > d[key]:
                    d[key] = y
            else:
                d[key] = y
        return [(key[0], key[1], key[2], d[key]) for key in d.keys()] 

    '''
    Computes and sets the average of the times.
    '''
    def average(self):
        self.data = self.getAverage()

    '''
    Computes and sets the maximum of the times.
    '''
    def maximum(self):
        self.data = self.getMaximum()

    '''
    Computes and sets the minimum of the times.
    '''
    def minimum(self):
        self.data = self.getMinimum()

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
    def scatter(self, system, task, color, marker='^', label=None, fillstyle=None):
        if label is None:
            label = system
        xs = []
        ys = []
        for x,y in self.filterBySystemAndTask(system,task):
            xs.append(x)
            ys.append(y)
        self.ax.plot(xs,ys,marker=marker,label=label,color=color,fillstyle=fillstyle,linestyle='')
        self.ax.set_ylabel('Time [s]')

    '''
    Sets general visual parameter and shows the plot.
    '''
    def show(self, xlabel, xticks=None, filename='figure.pgf', top=None):
        self.ax.set_ylim(bottom=0)
        self.ax.set_ylim(bottom=0, top=top)
        if xticks is not None:
            self.ax.set_xticks(xticks)

        self.ax.set_xlabel(xlabel)
        self.ax.legend(loc='upper left', bbox_to_anchor=(0, 1), fancybox=True)

        plt.tight_layout()
        # plt.show(block=self.fig)
        plt.savefig(filename)

    '''
    Get the used times for a given system and task
    @param{str} system the system to get the times for
    @param{str} task   the task to get the times for
    @return{float[]}   array of times
    '''
    def getTime(self, system, task):
        return [time for _,time in self.filterBySystemAndTask(system,task)]

    '''
    Plot a cactus plot for a given system and task
    @param{str} system the system to create plot for
    @param{str} task   the task to create plot for
    '''
    def scatterCactus(self, system, task, color, marker='^', timeBudget=12000, label=None, useMinutes=False, fillstyle=None):
        if label == None:
            label = system
        times = self.getTime(system, task)
        times.sort()
        xs = []
        ys = []
        acc = 0
        count = 0
        for time in times:
            count += 1
            acc += time
            if acc > timeBudget:
                break
            xs.append(count)
            ys.append(acc)
        # self.ax.scatter(xs,ys,marker,label=label,color=color)
        if useMinutes:
            self.ax.set_ylabel('Time [min]')
            ys = [secs / 60 for secs in ys]
        else:
            self.ax.set_ylabel('Time [s]')
        self.ax.plot(xs,ys,marker=marker,linestyle='',label=label,color=color,fillstyle=fillstyle)


if __name__ == '__main__':
    name = 'Benchmark name'

    CW_FILE = '../crossword/results.txt'
    SM_FILE = '../stable-marriage/results.txt'
    VA_FILE = '../visit-all/results.txt'

    # specify the input file with the data
    filename = CW_FILE

    rc('text', usetex=True)
    rc('font', family = 'serif', serif = 'cmr10', size=9)
    rc('legend', fontsize=8)
    rc('lines', markersize=5)
    rc('text.latex', preamble=r'\usepackage{cmbright}')

    marker_rulewerk = CARETLEFTBASE
    marker_rulewerk_2 = CARETRIGHTBASE
    marker_gringo = 'o'

    fillstyle_gringo = 'none'

    color_gringo = 'k'

    benchmarks = [(filename, showSolve) for filename in [CW_FILE, SM_FILE, VA_FILE] for showSolve in [True, False]]
    print(benchmarks)
    for filename, showSolve in benchmarks:
        # time.sleep(1)
        rc('figure', figsize=(4,2.5) if filename == CW_FILE else (3,2.5))
        benchmark = Benchmark(name, filename)
        benchmark.average()

        class_name = 'Crossword' if filename == CW_FILE else 'Stable marriage' if filename == SM_FILE else 'Visit all'

        if filename == CW_FILE:
        # create a scatter plot for crossword data
            xticks=range(0,16,3)
            xlabel='Crossword size'

            if showSolve:
                benchmark.scatter('clasp@rulewerk','solve-only', 'tab:blue', marker=marker_rulewerk, label='Clasp@Rulewerk-ASP')
                benchmark.scatter('clasp@rulewerk-fast','solve-only', 'tab:red', marker=marker_rulewerk_2, label='Clasp@Rulewerk-ASP\'')
                benchmark.scatter('clasp@gringo','solve-only', color_gringo, marker=marker_gringo, fillstyle=fillstyle_gringo, label='Clasp@Gringo')
                # benchmark.scatter('clasp@rulewerk-opt','solve-only', marker='x')

                # benchmark.scatter('clasp@shuf', 'solve-only', label='clasp@shuf')
                # benchmark.minimum()
                # benchmark.scatter('clasp@shuf', 'solve-only', label='clasp@shuf-min')
                # benchmark.maximum()
                # benchmark.scatter('clasp@shuf', 'solve-only', label='clasp@shuf-max')

            else:
                benchmark.scatter('rulewerk','ground', 'tab:blue', marker=marker_rulewerk, label='Rulewerk-ASP')
                benchmark.scatter('rulewerk-fast','ground', 'tab:red', marker=marker_rulewerk_2, label='Rulewerk-ASP\'')
                benchmark.scatter('gringo','ground', color_gringo, marker=marker_gringo, fillstyle=fillstyle_gringo, label='Gringo')

        else:
        # create a cactus plot for stable-marriage and visit-all data
            x_max = 20 if not showSolve else 10 if filename == SM_FILE else 18
            xticks = range(0,x_max+1,2)

            if showSolve:
                benchmark.scatterCactus('clasp@rulewerk','solve-only', 'tab:blue', marker=marker_rulewerk, label='Clasp@Rulewerk-ASP', useMinutes=True)
                benchmark.scatterCactus('clasp@rulewerk-fast','solve-only', 'tab:red', marker=marker_rulewerk_2, label='Clasp@Rulewerk-ASP\'', useMinutes=True)
                benchmark.scatterCactus('clasp@gringo','solve-only', color_gringo, marker=marker_gringo, fillstyle=fillstyle_gringo, label='Clasp@Gringo', useMinutes=True)
                # benchmark.scatter('clasp@rulewerk-opt','solve-only', marker='x')

                # benchmark.scatterCactus('clasp@shuf', 'solve-only', label='clasp@shuf', marker='3')
                # benchmark.minimum()
                # benchmark.scatterCactus('clasp@shuf', 'solve-only', label='clasp@shuf-min', marker='2')
                # benchmark.maximum()
                # benchmark.scatterCactus('clasp@shuf', 'solve-only', label='clasp@shuf-max', marker='1')
                xlabel = 'Number of solved instances'

            else:
                benchmark.scatterCactus('rulewerk', 'ground', 'tab:blue', marker=marker_rulewerk, label='Rulewerk-ASP')
                benchmark.scatterCactus('rulewerk-fast','ground', 'tab:red', marker=marker_rulewerk_2, label='Rulewerk-ASP\'')
                benchmark.scatterCactus('gringo','ground', color_gringo, marker=marker_gringo, fillstyle=fillstyle_gringo, label='Gringo')
                xlabel = 'Number of grounded instances'

        # show the plot
        benchmark.show(xticks=xticks, xlabel=xlabel, filename='{} - {}.pgf'.format(class_name, 'solve' if showSolve else 'ground'))


