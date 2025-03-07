# Codephy Visualizer

A web-based visualization tool for Codephy phylogenetic models.

## Features

- Interactive visualization of model structure
- Model validation against the Codephy schema
- Dependency analysis for model components
- Examples browser
- Visual representation of model relationships

## Installation

### Prerequisites

- Node.js (v14 or later)
- npm or yarn

### Setup

1. Clone this repository into a new directory in your project:

```bash
cd your-codephy-project
git clone https://github.com/yourusername/codephy-visualizer.git visualizer
cd visualizer
```

2. Install dependencies:

```bash
npm install
```

3. Create required directories:

```bash
mkdir -p public/js
```

4. Copy your Codephy schema and example files:

```bash
cp ../schema/codephy-schema.json schema/
cp ../examples/*.json examples/
```

5. Build and start the server:

```bash
npm run build-ui
npm start
```

6. Open your browser and navigate to http://localhost:3000

## Project Structure

```
codephy-visualizer/
├── examples/              # Example model JSON files
├── public/                # Static files served by Express
│   ├── index.html         # Main HTML page
│   └── js/                # Compiled JavaScript files
├── schema/                # Contains the Codephy schema
├── src/                   # React source files
├── package.json           # Project configuration
├── server.js              # Express server
└── validator.js           # Model validation logic
```

## Using the Visualizer

1. Open the web interface at http://localhost:3000
2. Upload a Codephy model JSON file or paste the JSON directly
3. View the model visualization in the Graph View tab
4. Check model details in the Model Details tab
5. Use the validation feature to check model correctness

## Integrating with Existing Codephy Projects

### Option 1: Standalone Service

Run the visualizer as a separate service alongside your existing Codephy tools.

### Option 2: Embedded Component

Import the React components into your existing frontend application if you have one.

### Option 3: Build Process Integration

Add the visualizer to your build pipeline to automatically generate visualizations for your models.

## API Endpoints

- `GET /api/examples` - List available example models
- `GET /api/examples/:filename` - Get a specific example model
- `POST /api/validate` - Validate a model against the schema
- `POST /api/analyze` - Analyze model structure and dependencies

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
