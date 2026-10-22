# AI-Powered Smart Formula Recognition & Algebraic Equation Solver

A Java Spring Boot mini-project demonstrating OOP principles and design patterns.

## OOP Concepts
- Encapsulation: private fields with getters/setters in all model classes
- Inheritance: abstract Equation base class extended by 9 subclasses
- Abstraction: 4 interfaces (EquationSolver, EquationClassifier, EquationService, OCRService)
- Polymorphism: SolverFactory returns correct solver at runtime

## Design Patterns
- Factory: SolverFactory, EquationFactory
- Strategy: LinearSolver, QuadraticSolver, PolynomialSolver, SimultaneousEquationSolver
- Service Layer: EquationServiceImpl, Tess4JOCRService

## Setup
1. Install Java 21
2. Install Maven 3.8+
3. Install Tesseract OCR and its English trained data (required for image input):
   - On Windows, use the installer linked from the [UB Mannheim Tesseract releases](https://github.com/UB-Mannheim/tesseract/wiki).
   - Include the English (`eng`) language data during installation.
   - The defaults in `src/main/resources/application.properties` expect
     `C:\Program Files\Tesseract-OCR\tesseract.exe` and
     `C:\Program Files\Tesseract-OCR\tessdata\eng.traineddata`.
   - If installed elsewhere, set `tesseract.executable` to the full path of
     `tesseract.exe` and `tesseract.datapath` to its `tessdata` directory. If
     the executable is on `PATH`, set `tesseract.executable=tesseract`.
   - Verify the installation in PowerShell with
     `& "C:\Program Files\Tesseract-OCR\tesseract.exe" --version` and check
     that `eng.traineddata` exists in the configured data directory.
4. Run: `mvn spring-boot:run`
5. Open: http://localhost:8080

## API Endpoints
- POST /api/equation/classify
- POST /api/equation/solve
- POST /api/ocr/extract
- GET  /api/equation/health

## Supported Equations
Solved: Linear, Quadratic, Cubic Polynomial, Simultaneous (2x2)
Recognized: Linear through higher-degree polynomial equations, simultaneous
linear and polynomial systems, rational, radical, absolute-value, biquadratic,
reciprocal, factored, homogeneous, symmetric, multivariable, and explicitly
integer-solution (Diophantine) equations. Newly recognized forms are classified
only; solving remains limited to the existing solver types.

OCR accepts PNG and JPEG images up to 5 MB. OCR extraction requires the
separately installed Tesseract executable and matching trained data; the
Maven dependencies do not install either prerequisite.
