import igraph
import argparse
import sys

'''
Write the graph as an asp encoding.

@param igraph.graph graph the graph to get the encoding for
@param file file to write the encoding to
'''
def to_asp(graph, f):
    # write vertices
    for vertex in graph.vs:
        print('vertex({}) .'.format(vertex.index), file=f)

    # write edges
    for edge in graph.es:
        print('edge({},{}) .'.format(edge.source, edge.target), file=f)


'''
Determine and generate the requsted graph.

@param ArgumentParser.parsed_arguments the parse command line arguments
'''
def generate_graph(args):
    n_v = args.vertices
    n_e = args.edges if args.edges else int(0.1*n_v*n_v)
    # print(n_v, n_e)
    
    if args.tree:
        return igraph.Graph.Tree(n_v, args.tree)
    elif args.erdos_renyi:
        return igraph.Graph.Erdos_Renyi(n_v, args.erdos_renyi)
    elif args.power_law:
        return igraph.Graph.Static_Power_Law(n_v, n_e, args.power_law)
    else:
        print('No graph type selected')

if __name__ == '__main__':
    parser = argparse.ArgumentParser('Generate graph instances as asp programm.')

    parser.add_argument('vertices', help='number of vertices', type=int, metavar='VERTICES')
    parser.add_argument('-f', '--file', help='the file to write the encoding to')
    parser.add_argument('-e', '--edges', help='number of edges', type=int, metavar='EDGES')

    parser.add_argument('-t', '--tree', help='generate a tree', nargs='?', default=False, const=2, type=int, metavar='CHILDREN')
    parser.add_argument('-er', '--erdos-renyi', help='generate a graph based on the erdos-renyi model', type=float, metavar='EDGE_PROB')
    parser.add_argument('-pl', '--power-law', help='generate a graph with a power law distribution', nargs='?', default=False, const=2, type=int, metavar='EXP')

    args = parser.parse_args()
    g = generate_graph(args)
    # print(g.degree_distribution())
    if args.file:
        with open(args.file, 'w') as f:
            to_asp(g, f)
    else:
        to_asp(g, sys.stdout)
     

    
