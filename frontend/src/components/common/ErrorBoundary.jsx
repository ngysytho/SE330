import React from "react";

export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error) {
    console.error(error);
  }

  render() {
    if (this.state.error) {
      return (
        <div className="grid min-h-screen place-items-center bg-discord-app p-6 text-discord-text">
          <div className="max-w-xl rounded-lg border border-rose-800 bg-discord-panel p-5">
            <h1 className="text-lg font-bold text-rose-300">Frontend crashed</h1>
            <p className="mt-2 text-sm text-[#dbdee1]">{this.state.error.message}</p>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
