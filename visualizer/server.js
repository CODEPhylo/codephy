// server.js
const express = require('express');
const path = require('path');
const fs = require('fs');
const cors = require('cors');
const bodyParser = require('body-parser');
const CodephyValidator = require('./validator');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(bodyParser.json({ limit: '5mb' }));
app.use(express.static(path.join(__dirname, 'public')));

// Initialize validator
const schemaPath = path.join(__dirname, '..', 'schema', 'codephy-schema.json');
let validator;

try {
  validator = new CodephyValidator(schemaPath);
  console.log('Validator initialized successfully');
} catch (error) {
  console.error('Failed to initialize validator:', error);
}

// Routes
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// API endpoint to validate a model
app.post('/api/validate', (req, res) => {
  if (!validator) {
    return res.status(500).json({
      success: false,
      error: 'Validator not initialized'
    });
  }
  
  const { model } = req.body;
  
  if (!model) {
    return res.status(400).json({
      success: false,
      error: 'No model provided'
    });
  }
  
  try {
    const result = validator.validateModel(model);
    res.json({
      success: true,
      result
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

// Get example models from the examples directory
app.get('/api/examples', (req, res) => {
  const examplesDir = path.join(__dirname, 'examples');
  
  fs.readdir(examplesDir, (err, files) => {
    if (err) {
      console.error('Error reading examples directory:', err);
      return res.status(500).json({
        success: false,
        error: 'Could not read examples directory'
      });
    }
    
    const examples = files
      .filter(file => file.endsWith('.json'))
      .map(file => {
        return {
          name: file,
          path: `/api/examples/${file}`
        };
      });
    
    res.json({
      success: true,
      examples
    });
  });
});

// Get a specific example model
app.get('/api/examples/:filename', (req, res) => {
  const { filename } = req.params;
  const filePath = path.join(__dirname, 'examples', filename);
  
  fs.readFile(filePath, 'utf8', (err, data) => {
    if (err) {
      console.error(`Error reading file ${filename}:`, err);
      return res.status(404).json({
        success: false,
        error: `Example ${filename} not found`
      });
    }
    
    try {
      const model = JSON.parse(data);
      res.json({
        success: true,
        model
      });
    } catch (error) {
      res.status(500).json({
        success: false,
        error: `Error parsing JSON: ${error.message}`
      });
    }
  });
});

// API endpoint to analyze model structure (dependency graph)
app.post('/api/analyze', (req, res) => {
  if (!validator) {
    return res.status(500).json({
      success: false,
      error: 'Validator not initialized'
    });
  }
  
  const { model } = req.body;
  
  if (!model) {
    return res.status(400).json({
      success: false,
      error: 'No model provided'
    });
  }
  
  try {
    // First validate the model
    const validationResult = validator.validateModel(model);
    
    if (!validationResult.valid) {
      return res.json({
        success: false,
        errors: validationResult.errors
      });
    }
    
    // Build dependency graph
    const graph = validator.buildDependencyGraph(model);
    
    // Convert Map to a JSON serializable format
    const dependencies = {};
    for (const [node, deps] of graph.entries()) {
      dependencies[node] = deps;
    }
    
    // Extract node types
    const nodeTypes = {};
    
    if (model.randomVariables) {
      Object.entries(model.randomVariables).forEach(([name, variable]) => {
        nodeTypes[name] = {
          type: 'random',
          distributionType: variable.distribution.type,
          generates: variable.distribution.generates
        };
      });
    }
    
    if (model.deterministicFunctions) {
      Object.entries(model.deterministicFunctions).forEach(([name, func]) => {
        nodeTypes[name] = {
          type: 'deterministic',
          functionType: func.function
        };
      });
    }
    
    res.json({
      success: true,
      analysis: {
        dependencies,
        nodeTypes
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

// Start the server
app.listen(PORT, () => {
  console.log(`Codephy visualization server running on http://localhost:${PORT}`);
});
