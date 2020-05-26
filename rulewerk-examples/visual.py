
import matplotlib.pyplot as plt

vlog = []
gringo = []
output = []

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
ax.scatter(x, y, label="Grounding")

# Gringo
x = [float(instance) for instance,_ in gringo]
y = [float(duration) for _,duration in gringo]
ax.scatter(x, y, color="r", label="Gringo")

ax.set_ylim(bottom=0)
ax.set_xlim(left=0)

ax.set_ylabel('Time [s]')
ax.set_xlabel('Crossword size')
ax.legend(loc='upper center', bbox_to_anchor=(0.5, 1), ncol=3, fancybox=True)

plt.show(fig)
