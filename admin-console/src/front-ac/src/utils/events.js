export const createEventOfType = (eventType, detail) => {
  return new CustomEvent(eventType, {detail});
};

export const dispatchEvent = (event) => {
  document.dispatchEvent(event);
};

export const createAndDispatchEventOfType = (eventType, detail) => {
  const event = createEventOfType(eventType, detail);
  dispatchEvent(event);
};

export const addEventListenerOfType = (eventType, func) => {
  document.addEventListener(eventType, function ({detail}) {
    func(detail);
  });
};

export const FILTER_REMOVAL = 'filterRemoval';