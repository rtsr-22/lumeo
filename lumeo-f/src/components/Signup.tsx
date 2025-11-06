import { Link } from "react-router-dom";

function Signup() {
  return (
    <>
      <div className="flex flex-col items-center justify-center min-h-screen bg-gray-100">
        <h1 className="text-2xl font-bold mb-4">Signup</h1>
        <form className="flex flex-col gap-3 w-80 bg-white p-6 rounded-lg shadow">
          <input
            type="text"
            placeholder="Username"
            className="border p-2 rounded"
          />
          <input
            type="email"
            placeholder="Email"
            className="border p-2 rounded"
          />
          <input
            type="password"
            placeholder="Password"
            className="border p-2 rounded"
          />
          <button className="bg-green-500 text-white py-2 rounded">
            Signup
          </button>
        </form>
        <p className="mt-4">
          Already have an account?{" "}
          <Link to="/login" className="text-blue-600">
            Login
          </Link>
        </p>
      </div>
    </>
  );
}

export default Signup;
