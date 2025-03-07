// Create this file at visualizer/public/js/app.js
const { useState, useEffect } = React;

// CodephyVisualizer Component
const CodephyVisualizer = () => {
  const [modelJson, setModelJson] = useState('');
  const [model, setModel] = useState(null);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('graph');
  
  // Parse the model when the JSON changes
  useEffect(() => {
    if (!modelJson) return;
    
    try {
      const parsed = JSON.parse(modelJson);
      setModel(parsed);
      setError(null);
    } catch (e) {
      setError(`Invalid JSON: ${e.message}`);
      setModel(null);
    }
  }, [modelJson]);

  // Handle file upload
  const handleFileUpload = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    
    const reader = new FileReader();
    reader.onload = (event) => {
      setModelJson(event.target.result);
    };
    reader.readAsText(file);
  };

  // Example model for demo
  const loadExampleModel = () => {
    const exampleModel = {
      "codephyVersion": "0.1",
      "model": "SimpleHKY",
      "metadata": {
        "title": "Simple HKY Model",
        "description": "A basic HKY substitution model with a Yule tree prior"
      },
      "randomVariables": {
        "birthRate": {
          "distribution": {
            "type": "Exponential",
            "generates": "REAL",
            "parameters": {
              "rate": 10.0
            }
          }
        },
        "kappa": {
          "distribution": {
            "type": "LogNormal",
            "generates": "REAL",
            "parameters": {
              "meanlog": 1.0,
              "sdlog": 1.25
            }
          }
        },
        "baseFreqs": {
          "distribution": {
            "type": "Dirichlet",
            "generates": "REAL_VECTOR",
            "parameters": {
              "alpha": [1.0, 1.0, 1.0, 1.0]
            }
          }
        },
        "tree": {
          "distribution": {
            "type": "Yule",
            "generates": "TREE",
            "parameters": {
              "birthRate": { "variable": "birthRate" }
            }
          }
        },
        "alignment": {
          "distribution": {
            "type": "PhyloCTMC",
            "generates": "ALIGNMENT",
            "parameters": {
              "tree": { "variable": "tree" },
              "Q": { "variable": "hkyModel" }
            }
          }
        }
      },
      "deterministicFunctions": {
        "hkyModel": {
          "function": "hky",
          "arguments": {
            "kappa": { "variable": "kappa" },
            "frequencies": { "variable": "baseFreqs" }
          }
        }
      }
    };
    
    setModelJson(JSON.stringify(exampleModel, null, 2));
  };

  return (
    <div className="w-full max-w-6xl mx-auto p-4">
      <div className="mb-6 bg-white shadow rounded-lg">
        <div className="px-4 py-5 border-b border-gray-200 sm:px-6">
          <h3 className="text-lg leading-6 font-medium text-gray-900">
            Codephy Model Visualizer
          </h3>
        </div>
        <div className="px-4 py-5 sm:p-6">
          <div className="mb-4 space-y-2">
            <div className="flex items-center space-x-4">
              <label className="inline-block text-sm font-medium">
                <span className="px-4 py-2 bg-blue-600 text-white rounded cursor-pointer hover:bg-blue-700">
                  Upload Model JSON
                </span>
                <input 
                  type="file" 
                  accept=".json" 
                  onChange={handleFileUpload} 
                  className="hidden" 
                />
              </label>
              <button 
                className="px-4 py-2 bg-gray-600 text-white rounded hover:bg-gray-700"
                onClick={loadExampleModel}
              >
                Load Example
              </button>
            </div>
            {error && (
              <div className="p-3 bg-red-100 border border-red-300 text-red-700 rounded">
                {error}
              </div>
            )}
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Left panel: Input */}
            <div>
              <h3 className="text-lg font-medium mb-2">Model JSON</h3>
              <textarea
                className="w-full h-96 p-2 font-mono text-sm border border-gray-300 rounded"
                value={modelJson}
                onChange={(e) => setModelJson(e.target.value)}
                placeholder="Paste your Codephy model JSON here..."
              ></textarea>
            </div>
            
            {/* Right panel: Visualization */}
            <div>
              <div className="flex border-b mb-4">
                <button
                  onClick={() => setActiveTab('graph')}
                  className={`py-2 px-4 ${activeTab === 'graph' ? 'border-b-2 border-blue-500 font-medium' : ''}`}
                >
                  Graph View
                </button>
                <button
                  onClick={() => setActiveTab('details')}
                  className={`py-2 px-4 ${activeTab === 'details' ? 'border-b-2 border-blue-500 font-medium' : ''}`}
                >
                  Model Details
                </button>
              </div>
              
              {model && activeTab === 'graph' && (
                <ModelGraph model={model} />
              )}
              
              {model && activeTab === 'details' && (
                <ModelDetails model={model} />
              )}
              
              {!model && (
                <div className="h-96 flex items-center justify-center border border-gray-300 rounded bg-gray-50">
                  <p className="text-gray-500">
                    {error ? 'Fix the JSON error to visualize the model' : 'Upload or paste a Codephy model to visualize'}
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

// Simplified Model Graph component with clear node management
const ModelGraph = ({ model }) => {
  if (!model || !model.randomVariables) {
    return <p>No model data available</p>;
  }
  
  // Extract nodes and edges for layout
  const layoutNodes = [];
  const layoutEdges = [];
  
  // Add random variables as single nodes for layout
  Object.entries(model.randomVariables).forEach(([name, variable]) => {
    const type = variable.distribution.type;
    const generates = variable.distribution.generates;
    
    layoutNodes.push({
      id: name,
      type: 'random',
      distributionType: type,
      generates: generates
    });
    
    // Find dependencies in the parameters
    const parameters = variable.distribution.parameters;
    if (parameters) {
      Object.entries(parameters).forEach(([paramName, param]) => {
        if (param && typeof param === 'object' && param.variable) {
          layoutEdges.push({
            from: param.variable,
            to: name,
            label: paramName
          });
        }
      });
    }
  });
  
  // Add deterministic functions as single nodes for layout
  if (model.deterministicFunctions) {
    Object.entries(model.deterministicFunctions).forEach(([name, func]) => {
      layoutNodes.push({
        id: name,
        type: 'deterministic',
        functionType: func.function
      });
      
      // Find dependencies in the arguments
      const args = func.arguments;
      if (args) {
        Object.entries(args).forEach(([argName, value]) => {
          if (value && typeof value === 'object' && value.variable) {
            layoutEdges.push({
              from: value.variable,
              to: name,
              label: argName
            });
          }
        });
      }
    });
  }
  
  // Calculate layout using single nodes
  const sortedNodes = topologicalSort(layoutNodes, layoutEdges);
  const nodeLevels = computeNodeLevels(sortedNodes, layoutEdges);
  const singleNodePositions = calculateNodePositions(layoutNodes, nodeLevels, layoutEdges);
  
  // Build render elements from layout results
  const renderNodes = [];
  const renderEdges = [];
  
  // For each node in the layout, create a generator and variable node
  layoutNodes.forEach(node => {
    const nodeId = node.id;
    const nodePos = singleNodePositions[nodeId];
    
    if (!nodePos) {
      console.error(`Missing position for layout node: ${nodeId}`);
      return;
    }
    
    // Create generator node (distribution or function)
    const genNode = {
      id: `gen_${nodeId}`,
      displayName: nodeId,
      nodeType: 'generator',
      type: node.type === 'random' ? 'distribution' : 'function',
      label: node.type === 'random' ? node.distributionType : node.functionType,
      position: {
        x: nodePos.x,
        y: nodePos.y - 25
      }
    };
    
    // Create variable node
    const varNode = {
      id: `var_${nodeId}`,
      displayName: nodeId,
      nodeType: 'variable',
      type: node.type,
      distributionType: node.distributionType,
      functionType: node.functionType,
      generates: node.generates,
      position: {
        x: nodePos.x,
        y: nodePos.y + 25
      }
    };
    
    // Add both nodes to render list
    renderNodes.push(genNode);
    renderNodes.push(varNode);
    
    // Add edge connecting generator to variable
    renderEdges.push({
      from: genNode.id,
      to: varNode.id,
      isInternal: true
    });
  });
  
  // Add the external connections between nodes
  layoutEdges.forEach(edge => {
    const fromVariableId = `var_${edge.from}`;
    const toGeneratorId = `gen_${edge.to}`;
    
    renderEdges.push({
      from: fromVariableId,
      to: toGeneratorId,
      label: edge.label
    });
  });
  
  // Node color based on type
  const getNodeColor = (node) => {
    if (node.nodeType === 'generator') {
      return node.type === 'distribution' ? '#9E9E9E' : '#607D8B';
    }
    
    if (node.type === 'random') {
      if (node.generates === 'TREE') return '#8BC34A';
      if (node.generates === 'ALIGNMENT') return '#9C27B0';
      return '#2196F3';
    }
    return '#FF9800'; // deterministic
  };
  
  // Calculate SVG dimensions
  const allPositions = renderNodes.map(node => node.position);
  const minX = Math.min(...allPositions.map(pos => pos.x)) - 40;
  const maxX = Math.max(...allPositions.map(pos => pos.x)) + 40;
  const minY = Math.min(...allPositions.map(pos => pos.y));
  const maxY = Math.max(...allPositions.map(pos => pos.y));
  
  const width = Math.max(600, maxX - minX);
  const height = Math.max(400, maxY - minY);
  
  // Draw edges between nodes
  const drawEdge = (edge) => {
    const fromNode = renderNodes.find(n => n.id === edge.from);
    const toNode = renderNodes.find(n => n.id === edge.to);
    
    if (!fromNode || !toNode) {
      console.error(`Cannot find nodes for edge: ${edge.from} -> ${edge.to}`);
      return null;
    }
    
    const start = fromNode.position;
    const end = toNode.position;
    
    if (edge.isInternal) {
      // Direct straight line for internal connections
      return (
        <line
          x1={start.x}
          y1={start.y + 12}
          x2={end.x}
          y2={end.y - 18}
          stroke="#999"
          strokeWidth="1.5"
        />
      );
    }
    
    // For external connections, use a curved line
    const midX = (start.x + end.x) / 2;
    const midY = (start.y + end.y) / 2;
    
    // Calculate control points for a bezier curve
    const controlPoint = {
      x: midX,
      y: midY - 10
    };
    
    // Calculate path
    const path = `M ${start.x},${start.y} Q ${controlPoint.x},${controlPoint.y} ${end.x},${end.y}`;
    
    // Calculate position for the label
    const labelX = midX;
    const labelY = midY + 20;
    
    return (
      <g>
        <path 
          d={path} 
          fill="none" 
          stroke="#999" 
          strokeWidth="1.5" 
          markerEnd="url(#arrowhead)"
        />
        {edge.label && (
          <text
            x={labelX}
            y={labelY}
            fontSize="9"
            textAnchor="middle"
            fill="#666"
            dy="-5"
          >
            <tspan>{edge.label}</tspan>
          </text>
        )}
      </g>
    );
  };
  
  // Render a node
  const drawNode = (node) => {
    const pos = node.position;
    
    if (node.nodeType === 'generator') {
      if (node.type === 'distribution') {
        // Square shape for distributions
        return (
          <g>
            <rect
              x={pos.x - 12}
              y={pos.y - 12}
              width="24"
              height="24"
              fill={getNodeColor(node)}
              stroke="#333"
              strokeWidth="1"
              className="node-shape"
            />
            <text
              x={pos.x + 16}
              y={pos.y}
              textAnchor="start"
              dominantBaseline="middle"
              fill="#333"
              fontSize="10"
            >
              {node.label}
            </text>
          </g>
        );
      } else {
        // Diamond shape for functions
        return (
          <g>
            <polygon
              points={`${pos.x},${pos.y-12} ${pos.x+12},${pos.y} ${pos.x},${pos.y+12} ${pos.x-12},${pos.y}`}
              fill={getNodeColor(node)}
              stroke="#333"
              strokeWidth="1"
              className="node-shape"
            />
            <text
              x={pos.x + 16}
              y={pos.y}
              textAnchor="start"
              dominantBaseline="middle"
              fill="#333"
              fontSize="10"
            >
              {node.label}
            </text>
          </g>
        );
      }
    } else {
      // Circle for variables
      return (
        <g>
          <circle
            cx={pos.x}
            cy={pos.y}
            r="30"
            fill={getNodeColor(node)}
            stroke="#333"
            strokeWidth="1"
            className="node-circle"
          />
          <text
            x={pos.x}
            y={pos.y}
            textAnchor="middle"
            dominantBaseline="middle"
            fill="white"
            fontSize="11"
            fontWeight="bold"
          >
            {node.displayName}
          </text>
          <text
            x={pos.x}
            y={pos.y + 20}
            textAnchor="middle"
            fontSize="9"
            fill="#333"
          >
            {node.generates || ''}
          </text>
        </g>
      );
    }
  };
  
return (
  <div className="border rounded p-4 h-96 overflow-auto bg-white">
    <svg 
      width="100%" 
      height={height} 
      viewBox={`${minX} ${minY} ${width} ${height}`}
      className="mx-auto"
    >
      {/* Draw edges */}
      {renderEdges.map((edge, i) => (
        <React.Fragment key={`edge-${i}`}>
          {drawEdge(edge)}
        </React.Fragment>
      ))}
      
      {/* Draw nodes */}
      {renderNodes.map((node, i) => (
        <React.Fragment key={node.id}>
          {drawNode(node)}
        </React.Fragment>
      ))}
      
      
      {/* Arrow marker definition */}
      <defs>
        <marker
          id="arrowhead"
          markerWidth="10"
          markerHeight="7"
          refX="9"
          refY="3.5"
          orient="auto"
        >
          <polygon points="0 0, 10 3.5, 0 7" fill="#999" />
        </marker>
      </defs>
    </svg>
    
  </div>
);
};


// Calculate positions for the layout
function calculateNodePositions(nodes, levels, edges) {
  const positions = {};
  const levelHeight = 120; 
  
  // Group nodes by level
  const levelNodes = {};
  nodes.forEach(node => {
    const level = levels[node.id] || 0;
    
    if (!levelNodes[level]) {
      levelNodes[level] = [];
    }
    levelNodes[level].push(node.id);
  });
  
  // Build parent-to-child map
  const parentToChild = {};
  edges.forEach(edge => {
    if (!parentToChild[edge.from]) {
      parentToChild[edge.from] = [];
    }
    parentToChild[edge.from].push(edge.to);
  });
  
  // Sort levels
  const allLevels = Object.keys(levelNodes).map(Number).sort();
  
  // First pass: Place nodes at each level with even spacing
  for (const level of allLevels) {
    const nodesAtLevel = levelNodes[level] || [];
    
    // Skip empty levels
    if (nodesAtLevel.length === 0) continue;
    
    const count = nodesAtLevel.length;
    const spacing = 120;
    
    nodesAtLevel.forEach((nodeId, index) => {
      positions[nodeId] = {
        x: spacing * (index + 1),
        y: level * levelHeight
      };
    });
  }
  
  // Second pass: Adjust positions to reduce crossings (bottom-up)
  for (let i = allLevels.length - 1; i >= 0; i--) {
    const level = allLevels[i];
    const nodesAtLevel = levelNodes[level] || [];
    
    nodesAtLevel.forEach(nodeId => {
      // If this node has children, adjust its position
      const children = parentToChild[nodeId] || [];
      
      if (children.length > 0) {
        // Calculate average x-position of children that have positions
        const positionedChildren = children.filter(childId => positions[childId]);
        
        if (positionedChildren.length > 0) {
          const avgX = positionedChildren.reduce((sum, childId) => {
            return sum + positions[childId].x;
          }, 0) / positionedChildren.length;
          
          // Update this node's x-position
          if (positions[nodeId]) {
            positions[nodeId].x = avgX;
          }
        }
      }
    });
  }
  
  // Third pass: Check for overlaps and adjust
  for (const level of allLevels) {
    const nodesAtLevel = levelNodes[level] || [];
    
    // Skip levels with 0-1 nodes
    if (nodesAtLevel.length <= 1) continue;
    
    // Sort nodes by x-position
    const sortedNodes = [...nodesAtLevel].sort((a, b) => {
      if (!positions[a] || !positions[b]) return 0;
      return positions[a].x - positions[b].x;
    });
    
    // Check for overlaps and adjust
    const minSpacing = 100;
    
    for (let i = 1; i < sortedNodes.length; i++) {
      const current = sortedNodes[i];
      const previous = sortedNodes[i-1];
      
      if (!positions[current] || !positions[previous]) continue;
      
      const currentX = positions[current].x;
      const previousX = positions[previous].x;
      
      // If nodes are too close, shift the current one
      if (currentX - previousX < minSpacing) {
        positions[current].x = previousX + minSpacing;
      }
    }
  }
  
  return positions;
}

// Helper functions for topological sort and level computation remain unchanged
function topologicalSort(nodes, edges) {
  // Build adjacency list
  const graph = {};
  nodes.forEach(node => {
    graph[node.id] = [];
  });
  
  edges.forEach(edge => {
    if (graph[edge.from]) {
      graph[edge.from].push(edge.to);
    }
  });
  
  // Find all nodes with no incoming edges (roots)
  const rootNodes = nodes.filter(node => 
    !edges.some(edge => edge.to === node.id)
  ).map(node => node.id);
  
  const visited = new Set();
  const sorted = [];
  
  function visit(nodeId) {
    if (visited.has(nodeId)) return;
    visited.add(nodeId);
    
    const neighbors = graph[nodeId] || [];
    for (const neighbor of neighbors) {
      visit(neighbor);
    }
    
    sorted.unshift(nodeId); // Add to the beginning
  }
  
  // Start with root nodes
  for (const rootNode of rootNodes) {
    visit(rootNode);
  }
  
  // Process any remaining nodes (in case of cycles)
  for (const node of nodes) {
    visit(node.id);
  }
  
  return sorted;
}

// Compute node levels (distance from root)
function computeNodeLevels(sortedNodes, edges) {
  const levels = {};
  
  // Initialize all nodes at level 0
  sortedNodes.forEach(nodeId => {
    levels[nodeId] = 0;
  });
  
  // Build child-to-parent map
  const parentMap = {};
  edges.forEach(edge => {
    if (!parentMap[edge.to]) {
      parentMap[edge.to] = [];
    }
    parentMap[edge.to].push(edge.from);
  });
  
  // Compute levels (parents must be higher level than children)
  let changed = true;
  while (changed) {
    changed = false;
    for (const nodeId of sortedNodes) {
      const parents = parentMap[nodeId] || [];
      
      for (const parentId of parents) {
        const newLevel = levels[parentId] + 1;
        if (newLevel > levels[nodeId]) {
          levels[nodeId] = newLevel;
          changed = true;
        }
      }
    }
  }
  
  return levels;
}

// Model details component
const ModelDetails = ({ model }) => {
  if (!model) return null;
  
  return (
    <div className="border rounded p-4 h-96 overflow-auto bg-white">
      {model.metadata && (
        <div className="mb-4">
          <h3 className="text-lg font-medium">{model.metadata.title || 'Untitled Model'}</h3>
          <p className="text-sm text-gray-600">{model.metadata.description || 'No description'}</p>
          {model.metadata.authors && model.metadata.authors.length > 0 && (
            <p className="text-xs text-gray-500 mt-1">
              Authors: {model.metadata.authors.map(a => a.name).join(', ')}
            </p>
          )}
        </div>
      )}
      
      {model.randomVariables && (
        <div className="mb-4">
          <h4 className="font-medium text-md mb-2">Random Variables</h4>
          <div className="space-y-2">
            {Object.entries(model.randomVariables).map(([name, variable]) => (
              <div key={name} className="border p-2 rounded">
                <div className="font-medium">{name}</div>
                <div className="text-sm">
                  <span className="text-blue-600">{variable.distribution.type}</span>
                  {' → '}
                  <span className="text-gray-600">{variable.distribution.generates}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
      
      {model.deterministicFunctions && (
        <div className="mb-4">
          <h4 className="font-medium text-md mb-2">Deterministic Functions</h4>
          <div className="space-y-2">
            {Object.entries(model.deterministicFunctions).map(([name, func]) => (
              <div key={name} className="border p-2 rounded">
                <div className="font-medium">{name}</div>
                <div className="text-sm">
                  <span className="text-orange-600">{func.function}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
      
      {model.constraints && model.constraints.length > 0 && (
        <div>
          <h4 className="font-medium text-md mb-2">Constraints</h4>
          <div className="space-y-2">
            {model.constraints.map((constraint, i) => (
              <div key={i} className="border p-2 rounded text-sm">
                {constraint.type}: {constraint.left || constraint.variable} 
                {constraint.right && ` → ${constraint.right}`}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

// Render the app
ReactDOM.render(
  <CodephyVisualizer />,
  document.getElementById('root')
);