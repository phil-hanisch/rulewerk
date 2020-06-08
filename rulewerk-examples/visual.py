
import matplotlib.pyplot as plt

# component --> (size --> list(duration))
results = {}

# get all results
with open('results_with_duplicates') as f:
    for line in f:
        size, component, duration = [elem.strip() for elem in line.split('#')[1:]]
        if ":" in duration:
            mins, secs = duration.split(":")
            duration = 60 * float(mins) + float(secs)
        else:
            duration = float(duration)
        size = int(size.lstrip("size"))

        results_for_component = results.get(component, {})
        results_for_size = results_for_component.get(size, [])
        results_for_size.append(duration)
        results_for_component[size] = results_for_size
        results[component] = results_for_component

# compute average
averages = {}
for c in results.keys():
    averages_by_component = {}
    for s in results[c].keys():
        results_for_size = results[c][s]
        averages_by_component[s] = (sum(results_for_size) / len(results_for_size))
    averages[c] = averages_by_component

for c in averages.keys():
    for s in averages[c].keys():
        print(c, s, averages[c][s])
        
# show results
fig, ax = plt.subplots()  # Create a figure containing a single axes.
components = ['Gringo', 'VLog', 'Complete', 'CompleteOverall']
colors = {'Gringo': 'k', 'VLog': 'r', 'Complete': 'green', 'CompleteOverall': 'blue'}
markers = {'Gringo': 'o'}
labels = {}
for component in components:
    x = []
    y = []
    averages_by_component = averages.get(component)
    for size in averages_by_component.keys():
        x.append(size)
        y.append(averages_by_component.get(size))
    # ax.scatter(x, y, label=labels.get(component, component))
    print(x, y, component)
    ax.scatter(x, y, label=labels.get(component, component), color=colors.get(component, 'k'), marker=markers.get(component, 'o'))

ax.set_ylim(bottom=0)
ax.set_xlim(left=0)

ax.set_ylabel('Time [s]')
ax.set_xlabel('Crossword size')
ax.legend(loc='upper center', bbox_to_anchor=(0.5, 1), ncol=3, fancybox=True)

plt.show(fig)
