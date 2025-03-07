// validator.js
const Ajv = require('ajv');
const addFormats = require('ajv-formats');
const fs = require('fs');
const path = require('path');

class CodephyValidator {
  constructor(schemaPath) {
    this.schemaPath = schemaPath;
    this.ajv = new Ajv({ 
      allErrors: true,
      validateSchema: false // Skip validating the schema itself
    });
    addFormats(this.ajv);
    this.loadSchema();
  }
  loadSchema() {
    try {
      const schemaContent = fs.readFileSync(this.schemaPath, 'utf8');
      this.schema = JSON.parse(schemaContent);
      this.validate = this.ajv.compile(this.schema);
      console.log('Schema loaded successfully');
    } catch (error) {
      console.error('Error loading schema:', error);
      throw error;
    }
  }

  validateModel(modelJson) {
    let model;
    
    // Parse the model JSON if it's a string
    if (typeof modelJson === 'string') {
      try {
        model = JSON.parse(modelJson);
      } catch (error) {
        return {
          valid: false,
          errors: [{ message: `Invalid JSON: ${error.message}` }]
        };
      }
    } else {
      model = modelJson;
    }
    
    // Validate against schema
    const valid = this.validate(model);
    
    if (!valid) {
      return {
        valid: false,
        errors: this.validate.errors
      };
    }
    
    // Additional semantic validation (references, cycles, etc.)
    const semanticErrors = this.validateSemantics(model);
    
    if (semanticErrors.length > 0) {
      return {
        valid: false,
        errors: semanticErrors
      };
    }
    
    return {
      valid: true,
      model: model
    };
  }
  
  validateSemantics(model) {
    const errors = [];
    
    // Check that all referenced variables exist
    this.checkReferences(model, errors);
    
    // Check for circular dependencies
    this.checkCircularDependencies(model, errors);
    
    // Check that distributions match their 'generates' types
    this.checkDistributionTypes(model, errors);
    
    return errors;
  }
  
  checkReferences(model, errors) {
    const definedVariables = new Set();
    
    // Collect all defined variables and functions
    if (model.randomVariables) {
      Object.keys(model.randomVariables).forEach(varName => {
        definedVariables.add(varName);
      });
    }
    
    if (model.deterministicFunctions) {
      Object.keys(model.deterministicFunctions).forEach(funcName => {
        definedVariables.add(funcName);
      });
    }
    
    // Check references in random variables
    if (model.randomVariables) {
      Object.entries(model.randomVariables).forEach(([varName, variable]) => {
        if (variable.distribution && variable.distribution.parameters) {
          this.checkParameterReferences(variable.distribution.parameters, definedVariables, errors, `randomVariables.${varName}`);
        }
      });
    }
    
    // Check references in deterministic functions
    if (model.deterministicFunctions) {
      Object.entries(model.deterministicFunctions).forEach(([funcName, func]) => {
        if (func.arguments) {
          Object.entries(func.arguments).forEach(([argName, value]) => {
            if (typeof value === 'object' && value !== null && value.variable) {
              if (!definedVariables.has(value.variable)) {
                errors.push({
                  message: `Undefined reference: ${value.variable} in deterministicFunctions.${funcName}.arguments.${argName}`,
                  path: `deterministicFunctions.${funcName}.arguments.${argName}`
                });
              }
            }
          });
        }
      });
    }
    
    // Check references in constraints
    if (model.constraints) {
      model.constraints.forEach((constraint, index) => {
        if (constraint.left && !definedVariables.has(constraint.left)) {
          errors.push({
            message: `Undefined reference: ${constraint.left} in constraints[${index}].left`,
            path: `constraints[${index}].left`
          });
        }
        
        if (constraint.right && typeof constraint.right === 'string' && !definedVariables.has(constraint.right)) {
          errors.push({
            message: `Undefined reference: ${constraint.right} in constraints[${index}].right`,
            path: `constraints[${index}].right`
          });
        }
        
        if (constraint.variable && !definedVariables.has(constraint.variable)) {
          errors.push({
            message: `Undefined reference: ${constraint.variable} in constraints[${index}].variable`,
            path: `constraints[${index}].variable`
          });
        }
        
        if (constraint.variables) {
          constraint.variables.forEach((variable, varIndex) => {
            if (!definedVariables.has(variable)) {
              errors.push({
                message: `Undefined reference: ${variable} in constraints[${index}].variables[${varIndex}]`,
                path: `constraints[${index}].variables[${varIndex}]`
              });
            }
          });
        }
      });
    }
  }
  
  checkParameterReferences(parameters, definedVariables, errors, path) {
    if (!parameters) return;
    
    Object.entries(parameters).forEach(([paramName, value]) => {
      if (Array.isArray(value)) {
        value.forEach((item, index) => {
          if (typeof item === 'object' && item !== null) {
            if (item.variable && !definedVariables.has(item.variable)) {
              errors.push({
                message: `Undefined reference: ${item.variable} in ${path}.parameters.${paramName}[${index}]`,
                path: `${path}.parameters.${paramName}[${index}]`
              });
            }
          }
        });
      } else if (typeof value === 'object' && value !== null) {
        if (value.variable && !definedVariables.has(value.variable)) {
          errors.push({
            message: `Undefined reference: ${value.variable} in ${path}.parameters.${paramName}`,
            path: `${path}.parameters.${paramName}`
          });
        }
      }
    });
  }
  
  checkCircularDependencies(model, errors) {
    const graph = this.buildDependencyGraph(model);
    const visited = new Set();
    const stack = new Set();
    
    const dfs = (node, path = []) => {
      if (stack.has(node)) {
        const cycle = [...path.slice(path.indexOf(node)), node];
        errors.push({
          message: `Circular dependency detected: ${cycle.join(' → ')}`,
          path: `circular_dependency`
        });
        return;
      }
      
      if (visited.has(node)) return;
      
      visited.add(node);
      stack.add(node);
      path.push(node);
      
      const dependencies = graph.get(node) || [];
      for (const dep of dependencies) {
        dfs(dep, [...path]);
      }
      
      stack.delete(node);
    };
    
    for (const node of graph.keys()) {
      if (!visited.has(node)) {
        dfs(node);
      }
    }
  }
  
  buildDependencyGraph(model) {
    const graph = new Map();
    
    // Process random variables
    if (model.randomVariables) {
      Object.entries(model.randomVariables).forEach(([varName, variable]) => {
        const dependencies = [];
        
        if (variable.distribution && variable.distribution.parameters) {
          this.extractDependencies(variable.distribution.parameters, dependencies);
        }
        
        graph.set(varName, dependencies);
      });
    }
    
    // Process deterministic functions
    if (model.deterministicFunctions) {
      Object.entries(model.deterministicFunctions).forEach(([funcName, func]) => {
        const dependencies = [];
        
        if (func.arguments) {
          Object.values(func.arguments).forEach(value => {
            if (typeof value === 'object' && value !== null && value.variable) {
              dependencies.push(value.variable);
            }
          });
        }
        
        graph.set(funcName, dependencies);
      });
    }
    
    return graph;
  }
  
  extractDependencies(parameters, dependencies) {
    if (!parameters) return;
    
    Object.values(parameters).forEach(value => {
      if (Array.isArray(value)) {
        value.forEach(item => {
          if (typeof item === 'object' && item !== null && item.variable) {
            dependencies.push(item.variable);
          }
        });
      } else if (typeof value === 'object' && value !== null && value.variable) {
        dependencies.push(value.variable);
      }
    });
  }
  
  checkDistributionTypes(model, errors) {
    if (!model.randomVariables) return;
    
    // Define valid 'generates' types for each distribution
    const validTypes = {
      'LogNormal': ['REAL', 'REAL_VECTOR'],
      'Normal': ['REAL', 'REAL_VECTOR'],
      'Gamma': ['REAL', 'REAL_VECTOR'],
      'Beta': ['REAL', 'REAL_VECTOR'],
      'Exponential': ['REAL', 'REAL_VECTOR'],
      'Uniform': ['REAL', 'REAL_VECTOR'],
      'Dirichlet': ['REAL_VECTOR'],
      'MultivariateNormal': ['REAL_VECTOR'],
      'Mixture': ['REAL', 'REAL_VECTOR'],
      'PosteriorApproximation': ['REAL', 'REAL_VECTOR'],
      'Yule': ['TREE'],
      'BirthDeath': ['TREE'],
      'Coalescent': ['TREE'],
      'ConstrainedYule': ['TREE'],
      'PhyloCTMC': ['ALIGNMENT']
    };
    
    Object.entries(model.randomVariables).forEach(([varName, variable]) => {
      const dist = variable.distribution;
      if (!dist || !dist.type || !dist.generates) return;
      
      const allowedTypes = validTypes[dist.type];
      if (allowedTypes && !allowedTypes.includes(dist.generates)) {
        errors.push({
          message: `Invalid 'generates' type for ${dist.type} distribution: got ${dist.generates}, expected one of [${allowedTypes.join(', ')}]`,
          path: `randomVariables.${varName}.distribution.generates`
        });
      }
    });
  }
}

module.exports = CodephyValidator;

// Example usage:
// const validator = new CodephyValidator('./schema/codephy-schema.json');
// const result = validator.validateModel(modelJson);
// console.log(result);
