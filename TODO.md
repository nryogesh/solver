# TODO — Smart Equation Solver Debug/Fix

- [x] Fix frontend bug: `copySteps()` uses `event.target` but `event` is undefined.
  - [x] Update `src/main/resources/templates/index.html` to call `copySteps(this)`
  - [x] Update `src/main/resources/static/js/app.js` to accept a button element and update UI correctly
- [ ] Run `mvn test` (and/or build) to ensure backend/frontend compilation is unaffected
- [ ] Thorough manual verification checklist (after tests):
  - [ ] Identify + Solve (Linear)
  - [ ] Identify + Solve (Quadratic/Cubic as supported)
  - [ ] Simultaneous handling
  - [ ] Unsupported equation shows “not supported” UI
  - [ ] OCR extract flow populates OCR text and warns on invalid equation
  - [ ] “Copy” button works (no console errors) and clipboard content matches expected format
