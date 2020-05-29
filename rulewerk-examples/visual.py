
import matplotlib.pyplot as plt

vlog = []
gringo = []
output = []
minimal = []
optimised = []
improved = []
improvedOverall = []

with open('results.txt') as f:
    for line in f:
        instance, component, duration = [elem.strip() for elem in line.split('#')[1:]]
        instance = instance.lstrip("size")
        if component == 'VLog':
            vlog.append((instance, duration))
        elif component == 'Gringo':
            gringo.append((instance, duration))
        elif component == 'Output':
            output.append((instance, duration))
        elif component == 'Querying':
            minimal.append((instance, duration))
        elif component == 'Optimised':
            optimised.append((instance, duration))
        elif component == 'Improved':
            improved.append((instance, duration))
        elif component == 'ImprovedOverall':
            improvedOverall.append((instance, duration))
print("VLog: ", vlog)
print("Gringo: ", gringo)

fig, ax = plt.subplots()  # Create a figure containing a single axes.

# Vlog
x = [float(instance) for instance,_ in vlog]
y = [float(duration) for _,duration in vlog]
ax.scatter(x, y, color="c", label="VLog")

# Output
x = [float(instance) for instance,_ in output]
y = [float(duration) for _,duration in output]
ax.scatter(x, y, label="Unimproved")


# Gringo
x = [float(instance) for instance,_ in gringo]
y = [float(duration) for _,duration in gringo]
ax.scatter(x, y, color="r", label="Gringo")

# # Minimal
# x = [float(instance) for instance,_ in minimal]
# y = [float(duration) for _,duration in minimal]
# ax.scatter(x, y, label="Minimal")

# Answering
x = [float(instance) for instance,_ in optimised]
y = [float(duration) for _,duration in optimised]
ax.scatter(x, y, label="Calling VLog", marker="1")

# Improved
x = [float(instance) for instance,_ in improved]
y = [float(duration) for _,duration in improved]
ax.scatter(x, y, label="Optimised", marker="2")

# Overall
x = [float(instance) for instance,_ in improvedOverall]
y = [float(duration) for _,duration in improvedOverall]
ax.scatter(x, y, label="Overall", marker="2", color='k')

ax.set_ylim(bottom=0)
ax.set_xlim(left=0)

ax.set_ylabel('Time [s]')
ax.set_xlabel('Crossword size')
ax.legend(loc='upper center', bbox_to_anchor=(0.5, 1), ncol=3, fancybox=True)

plt.show(fig)
